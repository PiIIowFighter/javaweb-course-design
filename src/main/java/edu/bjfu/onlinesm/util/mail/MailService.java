package edu.bjfu.onlinesm.util.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;



public class MailService {

    private static final MailConfig CONFIG = MailConfig.load();
    private static final MailSender SENDER = new MailSender(CONFIG);

    public static MailConfig config() {
        return CONFIG;
    }

    public static void sendText(String to, String subject, String textBody) {
        send(to, subject, textBody, null, null);
    }

    public static void sendHtml(String to, String subject, String htmlBody) {
        send(to, subject, null, htmlBody, null);
    }

    public static void sendWithAttachments(String to, String subject, String textBody, List<File> attachmentFiles) {
        List<MailAttachment> atts = new ArrayList<>();
        if (attachmentFiles != null) {
            for (File f : attachmentFiles) {
                if (f == null || !f.exists() || !f.isFile()) continue;
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    
                    atts.add(new MailAttachment(f.getName(), null, bytes));
                } catch (Exception e) {
                    System.out.println("[Mail] read attachment failed: " + f + ", " + e.getMessage());
                }
            }
        }
        send(to, subject, textBody, null, atts);
    }


    public static void send(String to, String subject, String textBody, String htmlBody, List<MailAttachment> attachments) {
        try {
            SENDER.send(to, subject, textBody, htmlBody, attachments);
        } catch (Exception e) {
            System.out.println("[Mail] send failed: " + e.getMessage());
            
        }
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

