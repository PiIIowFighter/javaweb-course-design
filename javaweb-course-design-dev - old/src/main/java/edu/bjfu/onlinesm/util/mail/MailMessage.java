package edu.bjfu.onlinesm.util.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 邮件对象：支持 To/Cc、HTML 正文与附件。
 */
public class MailMessage {

    private final List<String> to = new ArrayList<>();
    private final List<String> cc = new ArrayList<>();
    private String subject;
    private String htmlBody;
    private final List<MailAttachment> attachments = new ArrayList<>();

    public MailMessage addTo(String email) {
        if (email != null && !email.trim().isEmpty()) {
            to.add(email.trim());
        }
        return this;
    }

    public MailMessage addCc(String email) {
        if (email != null && !email.trim().isEmpty()) {
            cc.add(email.trim());
        }
        return this;
    }

    public MailMessage subject(String subject) {
        this.subject = subject;
        return this;
    }

    public MailMessage htmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
        return this;
    }

    public MailMessage addAttachment(MailAttachment attachment) {
        if (attachment != null && attachment.getBytes() != null) {
            attachments.add(attachment);
        }
        return this;
    }

    public List<String> getTo() {
        return Collections.unmodifiableList(to);
    }

    public List<String> getCc() {
        return Collections.unmodifiableList(cc);
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public List<MailAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }
}
