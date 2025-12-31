package edu.bjfu.onlinesm.util.mail;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 在不引入 JavaMail 依赖的情况下，手工拼装 MIME 邮件内容。
 */
final class MimeMessageBuilder {

    private MimeMessageBuilder() {
    }

    static String buildRawMessage(MailConfig cfg, MailMessage msg) {
        String boundary = "----=_Part_" + UUID.randomUUID();
        String fromHeader = encodeAddress(cfg.fromName(), cfg.from());

        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())).append("\r\n");
        sb.append("From: ").append(fromHeader).append("\r\n");
        sb.append("To: ").append(join(msg.getTo())).append("\r\n");
        if (!msg.getCc().isEmpty()) {
            sb.append("Cc: ").append(join(msg.getCc())).append("\r\n");
        }
        sb.append("Subject: ").append(encodeHeader(msg.getSubject())).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");

        boolean hasAttachments = msg.getAttachments() != null && !msg.getAttachments().isEmpty();
        if (!hasAttachments) {
            sb.append("Content-Type: text/html; charset=UTF-8\r\n");
            sb.append("Content-Transfer-Encoding: 8bit\r\n\r\n");
            sb.append(msg.getHtmlBody() == null ? "" : msg.getHtmlBody());
            sb.append("\r\n");
            return sb.toString();
        }

        sb.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n\r\n");

        // 正文
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Type: text/html; charset=UTF-8\r\n");
        sb.append("Content-Transfer-Encoding: 8bit\r\n\r\n");
        sb.append(msg.getHtmlBody() == null ? "" : msg.getHtmlBody());
        sb.append("\r\n");

        // 附件
        for (MailAttachment a : msg.getAttachments()) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: ").append(a.getContentType()).append("; name=\"").append(encodeFilename(a.getFilename())).append("\"\r\n");
            sb.append("Content-Transfer-Encoding: base64\r\n");
            sb.append("Content-Disposition: attachment; filename=\"").append(encodeFilename(a.getFilename())).append("\"\r\n\r\n");
            sb.append(base64Lines(a.getBytes()));
            sb.append("\r\n");
        }

        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }

    private static String join(List<String> emails) {
        return String.join(", ", emails);
    }

    private static String encodeHeader(String s) {
        if (s == null) s = "";
        // RFC2047 B-encoding
        String b = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + b + "?=";
    }

    private static String encodeFilename(String s) {
        if (s == null || s.isEmpty()) return "attachment";
        // 简化：直接用 RFC2047 编码
        return encodeHeader(s);
    }

    private static String encodeAddress(String name, String email) {
        if (email == null) email = "";
        if (name == null || name.trim().isEmpty()) {
            return email;
        }
        return encodeHeader(name) + " <" + email + ">";
    }

    private static String base64Lines(byte[] bytes) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < b64.length()) {
            int end = Math.min(i + 76, b64.length());
            sb.append(b64, i, end).append("\r\n");
            i = end;
        }
        return sb.toString();
    }
}
