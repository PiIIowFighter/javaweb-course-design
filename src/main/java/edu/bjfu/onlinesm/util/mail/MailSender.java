package edu.bjfu.onlinesm.util.mail;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * JavaMail 版本的邮件发送器（替代原 Socket+SMTP 实现）。
 *
 * <p>优点：
 * - STARTTLS/SSL 由 JavaMail 处理，更稳定
 * - MIME/编码/附件更规范
 *
 * <p>依赖：WEB-INF/lib 下的 javax.mail.jar 与 activation.jar（或 activation-*.jar）。
 */
public class MailSender {

    private final MailConfig config;

    public MailSender(MailConfig config) {
        this.config = config;
    }

    /**
     * @param to          收件人（可用逗号分隔多个）
     * @param subject     主题
     * @param textBody    纯文本正文（可空）
     * @param htmlBody    HTML 正文（可空）
     * @param attachments 附件（可空；本项目附件是 byte[]，不依赖 File）
     */
    public void send(String to, String subject, String textBody, String htmlBody, List<MailAttachment> attachments) throws MessagingException {
        if (to == null || to.trim().isEmpty()) return;

        if (!config.isEnabled()) {
            System.out.println("[Mail] smtp.enabled=false, skip sending to " + to + " subject=" + subject);
            return;
        }
        if (isEmpty(config.getHost())) {
            System.out.println("[Mail] SMTP 未配置完整（host 为空），skip sending.");
            return;
        }

        Session session = buildSession();
        MimeMessage mm = new MimeMessage(session);

        // From
        String fromAddr = config.getFrom();
        try {
            if (!isEmpty(config.getFromName())) {
                mm.setFrom(new InternetAddress(fromAddr, config.getFromName(), StandardCharsets.UTF_8.name()));
            } else {
                mm.setFrom(new InternetAddress(fromAddr));
            }
        } catch (Exception e) {
            mm.setFrom(new InternetAddress(fromAddr));
        }

        // To（支持逗号分隔）
        for (InternetAddress ia : InternetAddress.parse(to, false)) {
            mm.addRecipient(Message.RecipientType.TO, ia);
        }

        mm.setSubject(nullToEmpty(subject), StandardCharsets.UTF_8.name());
        mm.setSentDate(new Date());

        boolean hasAtt = attachments != null && !attachments.isEmpty();
        boolean hasHtml = !isEmpty(htmlBody);
        boolean hasText = !isEmpty(textBody);

        if (!hasHtml && !hasText) {
            hasText = true;
            textBody = "";
        }

        if (!hasAtt) {
            // 无附件：直接发 text 或 html 或 alternative
            if (hasHtml && !hasText) {
                mm.setContent(htmlBody, "text/html; charset=UTF-8");
            } else if (!hasHtml) {
                mm.setText(textBody, StandardCharsets.UTF_8.name());
            } else {
                // text + html
                MimeMultipart alt = new MimeMultipart("alternative");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(textBody, StandardCharsets.UTF_8.name());
                alt.addBodyPart(textPart);

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
                alt.addBodyPart(htmlPart);

                mm.setContent(alt);
            }
        } else {
            // 有附件：mixed，正文放在第一个 part
            MimeMultipart mixed = new MimeMultipart("mixed");

            // body part（优先用 html+text alternative）
            MimeBodyPart body = new MimeBodyPart();
            if (hasHtml && hasText) {
                MimeMultipart alt = new MimeMultipart("alternative");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(textBody, StandardCharsets.UTF_8.name());
                alt.addBodyPart(textPart);

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
                alt.addBodyPart(htmlPart);

                body.setContent(alt);
            } else if (hasHtml) {
                body.setContent(htmlBody, "text/html; charset=UTF-8");
            } else {
                body.setText(textBody, StandardCharsets.UTF_8.name());
            }
            mixed.addBodyPart(body);

            // attachments（byte[]）
            for (MailAttachment a : attachments) {
                if (a == null || a.getBytes() == null) continue;

                MimeBodyPart part = new MimeBodyPart();
                String ct = isEmpty(a.getContentType()) ? "application/octet-stream" : a.getContentType();
                ByteArrayDataSource ds = new ByteArrayDataSource(a.getBytes(), ct);
                part.setDataHandler(new DataHandler(ds));

                String filename = isEmpty(a.getFilename()) ? "attachment" : a.getFilename();
                try {
                    part.setFileName(MimeUtility.encodeText(filename, StandardCharsets.UTF_8.name(), null));
                } catch (Exception e) {
                    part.setFileName(filename);
                }

                mixed.addBodyPart(part);
            }

            mm.setContent(mixed);
        }

        Transport.send(mm);
    }

    private Session buildSession() {
        Properties p = new Properties();

        p.put("mail.smtp.host", config.getHost());
        p.put("mail.smtp.port", String.valueOf(config.getPort()));

        if (config.isDebug()) {
            p.put("mail.debug", "true");
        }

        boolean auth = !isEmpty(config.getUsername());
        p.put("mail.smtp.auth", auth ? "true" : "false");

        if (config.isSsl()) {
            p.put("mail.smtp.ssl.enable", "true");
            p.put("mail.smtp.ssl.trust", config.getHost());
        } else if (config.isStarttls()) {
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.starttls.required", "true");
        }

        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "20000");
        p.put("mail.smtp.writetimeout", "20000");

        Authenticator authenticator = null;
        if (auth) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), nullToEmpty(config.getPassword()));
                }
            };
        }

        Session session = Session.getInstance(p, authenticator);
        session.setDebug(config.isDebug());
        return session;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
