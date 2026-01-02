package edu.bjfu.onlinesm.util.mail;

import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 统一维护邮件主题/正文模板（HTML）。
 */
public final class MailTemplates {

    private MailTemplates() {
    }

    private static String h(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String fmt(LocalDateTime t) {
        if (t == null) return "";
        return t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public static MailMessage submissionConfirmation(MailConfig cfg, User author, Manuscript m, String manuscriptCode) {
        String subject = "投稿确认：" + manuscriptCode;
        String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());
        String body = "<p>尊敬的作者 <b>" + h(author.getFullName()) + "</b>：</p>" +
                "<p>您的稿件已提交成功，系统稿件编号：<b>" + h(manuscriptCode) + "</b></p>" +
                "<p>稿件标题：" + h(m.getTitle()) + "</p>" +
                (detailUrl.isEmpty() ? "" : ("<p>查看稿件详情：<a href=\"" + h(detailUrl) + "\">" + h(detailUrl) + "</a></p>")) +
                "<p>此邮件由系统自动发送，请勿直接回复。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage unsubmitReturn(MailConfig cfg, User author, Manuscript m, String manuscriptCode, String issues, String guideUrl) {
        String subject = "退回修改通知：" + manuscriptCode;
        String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());
        StringBuilder body = new StringBuilder();
        body.append("<p>尊敬的作者 <b>").append(h(author.getFullName())).append("</b>：</p>");
        body.append("<p>您的稿件（").append(h(manuscriptCode)).append("）在形式审查中未通过，已退回修改。</p>");
        body.append("<p>稿件标题：").append(h(m.getTitle())).append("</p>");
        body.append("<hr/>");
        body.append("<h3>问题列表</h3>");
        if (issues != null && !issues.trim().isEmpty()) {
            body.append("<div style=\"background-color:#f9f9f9;padding:15px;border-left:4px solid #dc3545;margin:10px 0;\">");
            body.append("<p style=\"margin:0 0 10px 0;color:#dc3545;\"><b>以下检查项不符合标准：</b></p>");
            body.append("<ul style=\"margin:0;padding-left:20px;\">");
            String[] issueList = issues.split("；");
            for (String issue : issueList) {
                if (issue != null && !issue.trim().isEmpty()) {
                    body.append("<li style=\"margin-bottom:5px;\">").append(h(issue.trim())).append("</li>");
                }
            }
            body.append("</ul>");
            body.append("</div>");
        } else {
            body.append("<p><b>问题列表：</b>（编辑部未填写具体问题，请登录系统查看退回原因/补充材料）</p>");
        }
        body.append("<hr/>");
        body.append("<h3>修改指南</h3>");
        body.append("<p>请根据以下要求修改您的稿件：</p>");
        body.append("<ul>");
        body.append("<li><b>作者信息：</b>确保通讯作者邮箱应为机构邮箱（优先使用.edu或.org域名）</li>");
        body.append("<li><b>摘要字数：</b>摘要字数应在150-700字之间</li>");
        body.append("<li><b>正文字数：</b>正文字数应在3000-8000字之间</li>");
        body.append("<li><b>关键词：</b>关键词应在3-7个之间</li>");
        body.append("<li><b>注释编号：</b>注释编号应符合学术规范</li>");
        body.append("<li><b>图表格式：</b>图表应清晰可读，格式符合期刊要求</li>");
        body.append("<li><b>参考文献格式：</b>参考文献应按照期刊要求的格式排版</li>");
        body.append("</ul>");
        if (guideUrl != null && !guideUrl.trim().isEmpty()) {
            body.append("<p style=\"margin-top:15px;\"><b>详细修改指南：</b><a href=\"").append(h(guideUrl.trim())).append("\" style=\"color:#007bff;text-decoration:none;\">")
                    .append("点击查看格式指南文档").append("</a></p>");
        }
        body.append("<hr/>");
        body.append("<h3>后续操作</h3>");
        body.append("<p><b>查看待修改稿件：</b>请登录系统后，在稿件列表页面切换到 <b>\"Revision（待修改）\"</b> 标签页，即可查看被退回的稿件。</p>");
        body.append("<p>请完成修改后在系统中重新提交（Resubmit）。</p>");
        if (!detailUrl.isEmpty()) {
            body.append("<p>查看稿件详情：<a href=\"").append(h(detailUrl)).append("\" style=\"color:#007bff;text-decoration:none;\">")
                    .append(h(detailUrl)).append("</a></p>");
        }
        body.append("<p style=\"color:#6c757d;font-size:12px;margin-top:20px;\">此邮件由系统自动发送，请勿直接回复。</p>");
        return new MailMessage().subject(subject).htmlBody(wrap(body.toString()));
    }

    public static MailMessage reviewerInvitation(MailConfig cfg, User reviewer, Manuscript m, int reviewId, LocalDateTime dueAt) {
        String subject = "审稿邀请：稿件 #" + m.getManuscriptId();
        String inviteUrl = link(cfg.baseUrl(), "/reviewer/invitation?id=" + reviewId);
        String body = "<p>尊敬的审稿人 <b>" + h(reviewer.getFullName()) + "</b>：</p>" +
                "<p>编辑部邀请您为以下稿件提供审稿意见：</p>" +
                "<p><b>标题：</b>" + h(m.getTitle()) + "</p>" +
                "<p><b>摘要：</b></p><div style=\"border:1px solid #ddd;padding:8px;\">" + (m.getAbstractText() == null ? "" : m.getAbstractText()) + "</div>" +
                (dueAt == null ? "" : ("<p><b>截止日期：</b>" + h(fmt(dueAt)) + "</p>")) +
                (inviteUrl.isEmpty() ? "" : ("<p>查看邀请并接受/拒绝：<a href=\"" + h(inviteUrl) + "\">" + h(inviteUrl) + "</a></p>")) +
                "<p>此邮件由系统自动发送，请勿直接回复。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage reviewerRemind(MailConfig cfg, User reviewer, Manuscript m, Review r) {
        String subject = "催审提醒：稿件 #" + m.getManuscriptId();
        String inviteUrl = link(cfg.baseUrl(), "/reviewer/invitation?id=" + r.getReviewId());
        String body = "<p>尊敬的审稿人 <b>" + h(reviewer.getFullName()) + "</b>：</p>" +
                "<p>这是对您审稿任务的提醒：</p>" +
                "<p><b>标题：</b>" + h(m.getTitle()) + "</p>" +
                (r.getDueAt() == null ? "" : ("<p><b>截止日期：</b>" + h(fmt(r.getDueAt())) + "</p>")) +
                (inviteUrl.isEmpty() ? "" : ("<p>进入系统提交意见：<a href=\"" + h(inviteUrl) + "\">" + h(inviteUrl) + "</a></p>")) +
                "<p>感谢您的支持！此邮件由系统自动发送，请勿直接回复。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }


    public static MailMessage reviewerRemindCustom(MailConfig cfg,
                                                   User reviewer,
                                                   Manuscript m,
                                                   Review r,
                                                   String extraText) {
        String subject = "催审提醒：稿件 #" + m.getManuscriptId();
        String inviteUrl = link(cfg.baseUrl(), "/reviewer/invitation?id=" + r.getReviewId());

        StringBuilder body = new StringBuilder();
        body.append("<p>尊敬的审稿人 <b>").append(h(reviewer.getFullName())).append("</b>：</p>");

        if (extraText != null && !extraText.trim().isEmpty()) {
            body.append("<p>").append(h(extraText.trim())).append("</p>");
        } else {
            body.append("<p>这是对您审稿任务的提醒：</p>");
        }

        body.append("<p><b>标题：</b>").append(h(m.getTitle())).append("</p>");
        if (r.getDueAt() != null) {
            body.append("<p><b>截止日期：</b>").append(h(fmt(r.getDueAt()))).append("</p>");
        }
        if (!inviteUrl.isEmpty()) {
            body.append("<p>进入系统提交意见：<a href=\"")
                    .append(h(inviteUrl))
                    .append("\">")
                    .append(h(inviteUrl))
                    .append("</a></p>");
        }
        body.append("<p>感谢您的支持！此邮件由系统自动发送，请勿直接回复。</p>");

        return new MailMessage().subject(subject).htmlBody(wrap(body.toString()));
    }

    public static MailMessage reviewerResponseToEditor(MailConfig cfg, User editor, User reviewer, Manuscript m, boolean accepted) {
        String subject = "审稿邀请回应：" + (accepted ? "已接受" : "已拒绝") + "（稿件 #" + m.getManuscriptId() + "）";
        String body = "<p>编辑 <b>" + h(editor.getFullName()) + "</b>：</p>" +
                "<p>审稿人 <b>" + h(reviewer.getFullName()) + "</b>（" + h(reviewer.getEmail()) + "）" +
                (accepted ? "已接受" : "已拒绝") + "该稿件的审稿邀请。</p>" +
                "<p><b>稿件标题：</b>" + h(m.getTitle()) + "</p>" +
                "<p>此邮件由系统自动发送。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage assignEditorNotice(MailConfig cfg, User editor, User chief, Manuscript m, String chiefComment) {
        String subject = "新稿件指派：稿件 #" + m.getManuscriptId();
        String detailUrl = link(cfg.baseUrl(), "/editor/withEditor");
        StringBuilder body = new StringBuilder();
        body.append("<p>编辑 <b>").append(h(editor.getFullName())).append("</b>：</p>");
        body.append("<p>主编 <b>").append(h(chief.getFullName())).append("</b> 已将稿件指派给您处理。</p>");
        body.append("<p><b>稿件标题：</b>").append(h(m.getTitle())).append("</p>");
        if (chiefComment != null && !chiefComment.trim().isEmpty()) {
            body.append("<p><b>主编建议：</b></p><pre style=\"white-space:pre-wrap;\">")
                    .append(h(chiefComment.trim()))
                    .append("</pre>");
        }
        if (!detailUrl.isEmpty()) {
            body.append("<p>请登录系统处理：<a href=\"").append(h(detailUrl)).append("\">")
                    .append(h(detailUrl)).append("</a></p>");
        }
        body.append("<p>此邮件由系统自动发送，请勿直接回复。</p>");
        return new MailMessage().subject(subject).htmlBody(wrap(body.toString()));
    }

    public static MailMessage finalDecisionToAuthor(MailConfig cfg, User author, Manuscript m, String manuscriptCode, String decisionText) {
        String subject = "终审结果通知：" + manuscriptCode;
        String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());
        String body = "<p>尊敬的作者 <b>" + h(author.getFullName()) + "</b>：</p>" +
                "<p>您的稿件（" + h(manuscriptCode) + "）已给出终审结果：</p>" +
                "<p><b>结果：</b>" + h(decisionText) + "</p>" +
                "<p><b>标题：</b>" + h(m.getTitle()) + "</p>" +
                (detailUrl.isEmpty() ? "" : ("<p>查看稿件详情：<a href=\"" + h(detailUrl) + "\">" + h(detailUrl) + "</a></p>")) +
                "<p>附件为审稿意见汇总（如有）。此邮件由系统自动发送，请勿直接回复。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage finalDecisionToEditor(MailConfig cfg, User editor, Manuscript m, String decisionText) {
        String subject = "终审结果已发布：稿件 #" + m.getManuscriptId();
        String body = "<p>编辑 <b>" + h(editor.getFullName()) + "</b>：</p>" +
                "<p>主编已对稿件作出终审决策：</p>" +
                "<p><b>标题：</b>" + h(m.getTitle()) + "</p>" +
                "<p><b>结果：</b>" + h(decisionText) + "</p>" +
                "<p>此邮件由系统自动发送。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage retractNoticeToAuthor(MailConfig cfg, User author, Manuscript m, String manuscriptCode) {
        String subject = "撤稿通知：" + manuscriptCode;
        String body = "<p>尊敬的作者 <b>" + h(author.getFullName()) + "</b>：</p>" +
                "<p>您的稿件（" + h(manuscriptCode) + "）已被主编执行撤稿操作，系统已归档该稿件。</p>" +
                "<p><b>标题：</b>" + h(m.getTitle()) + "</p>" +
                "<p>如有疑问请联系编辑部。此邮件由系统自动发送，请勿直接回复。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage inviteNewReviewer(MailConfig cfg, User reviewer, String rawPassword) {
        String subject = "审稿人账户邀请";
        String loginUrl = link(cfg.baseUrl(), "/auth/login");
        String body = "<p>您好 <b>" + h(reviewer.getFullName()) + "</b>：</p>" +
                "<p>主编已邀请您成为本系统审稿人，现为您创建了账户：</p>" +
                "<p><b>用户名：</b>" + h(reviewer.getUsername()) + "</p>" +
                (rawPassword == null ? "" : ("<p><b>初始密码：</b>" + h(rawPassword) + "</p>")) +
                (loginUrl.isEmpty() ? "" : ("<p>登录入口：<a href=\"" + h(loginUrl) + "\">" + h(loginUrl) + "</a></p>")) +
                "<p>建议登录后尽快修改密码。此邮件由系统自动发送，请勿直接回复。</p>";
        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static byte[] buildReviewSummaryTxt(List<Review> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("Review Summary\n");
        sb.append("==============================\n\n");
        if (reviews == null || reviews.isEmpty()) {
            sb.append("(No reviews)\n");
            return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        int idx = 1;
        for (Review r : reviews) {
            if (r == null) continue;
            if (!"SUBMITTED".equalsIgnoreCase(r.getStatus())) continue;
            sb.append("#").append(idx++).append(" Reviewer: ").append(nz(r.getReviewerName())).append("\n");
            if (r.getRecommendation() != null) {
                sb.append("Recommendation: ").append(r.getRecommendation()).append("\n");
            }
            if (r.getScore() != null) {
                sb.append("Total Score: ").append(r.getScore()).append("\n");
            }
            if (r.getKeyEvaluation() != null) {
                sb.append("Key Evaluation:\n").append(r.getKeyEvaluation()).append("\n\n");
            }
            if (r.getContent() != null) {
                sb.append("Comments to Author:\n").append(r.getContent()).append("\n\n");
            }
            if (r.getConfidentialToEditor() != null) {
                sb.append("Confidential to Editor:\n").append(r.getConfidentialToEditor()).append("\n\n");
            }
            sb.append("------------------------------\n\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String wrap(String inner) {
        return "<html><body style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;\">" + inner + "</body></html>";
    }

    private static String link(String baseUrl, String path) {
        if (baseUrl == null) return "";
        String b = baseUrl.trim();
        if (b.isEmpty()) return "";
        if (!b.startsWith("http://") && !b.startsWith("https://")) {
            return ""; // 避免错误地址
        }
        if (path == null) path = "";
        if (!path.startsWith("/")) path = "/" + path;
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + path;
    }
}
