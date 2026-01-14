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
import java.util.Properties;


public class SmtpMailer {

    private final MailConfig cfg;

    public SmtpMailer(MailConfig cfg) {
        this.cfg = cfg;
    }

    public void send(MailMessage msg) throws MessagingException {
        if (msg == null) return;

        
        if (!cfg.enabled()) return;

        if (isEmpty(cfg.host())) {
            throw new MessagingException("SMTP 配置不完整：请填写 smtp.host");
        }
        if (msg.getTo() == null || msg.getTo().isEmpty()) {
            throw new MessagingException("收件人为空");
        }

        Session session = buildSession();
        MimeMessage mm = new MimeMessage(session);

        
        String fromAddr = cfg.getFrom();
        try {
            if (!isEmpty(cfg.getFromName())) {
                mm.setFrom(new InternetAddress(fromAddr, cfg.getFromName(), StandardCharsets.UTF_8.name()));
            } else {
                mm.setFrom(new InternetAddress(fromAddr));
            }
        } catch (Exception e) {
            mm.setFrom(new InternetAddress(fromAddr));
        }

        
        for (String to : msg.getTo()) {
            if (!isEmpty(to)) {
                mm.addRecipient(Message.RecipientType.TO, new InternetAddress(to.trim()));
            }
        }
        if (msg.getCc() != null) {
            for (String cc : msg.getCc()) {
                if (!isEmpty(cc)) {
                    mm.addRecipient(Message.RecipientType.CC, new InternetAddress(cc.trim()));
                }
            }
        }

        
        mm.setSubject(nullToEmpty(msg.getSubject()), StandardCharsets.UTF_8.name());
        mm.setSentDate(new Date());

        
        boolean hasAtt = msg.getAttachments() != null && !msg.getAttachments().isEmpty();

        if (!hasAtt) {
            
            String html = nullToEmpty(msg.getHtmlBody());
            mm.setContent(html, "text/html; charset=UTF-8");
        } else {
            MimeMultipart mixed = new MimeMultipart("mixed");

            
            MimeBodyPart body = new MimeBodyPart();
            body.setContent(nullToEmpty(msg.getHtmlBody()), "text/html; charset=UTF-8");
            mixed.addBodyPart(body);

            
            for (MailAttachment a : msg.getAttachments()) {
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

        
        p.put("mail.smtp.host", cfg.host());
        p.put("mail.smtp.port", String.valueOf(cfg.port()));

        
        if (cfg.debug()) {
            p.put("mail.debug", "true");
        }

        
        boolean auth = !isEmpty(cfg.username());
        p.put("mail.smtp.auth", auth ? "true" : "false");

        
        if (cfg.ssl()) {
            p.put("mail.smtp.ssl.enable", "true");
            
            p.put("mail.smtp.ssl.trust", cfg.host());
        } else if (cfg.startTls()) {
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
                    return new PasswordAuthentication(cfg.username(), nullToEmpty(cfg.password()));
                }
            };
        }

        Session session = Session.getInstance(p, authenticator);
        session.setDebug(cfg.debug());
        return session;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

