package edu.bjfu.onlinesm.util.mail;

import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


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

    private static String person(User u) {
        if (u == null) return "用户";
        String name = u.getFullName();
        if (name == null || name.trim().isEmpty()) name = u.getUsername();
        if (name == null || name.trim().isEmpty()) name = "用户";
        return h(name.trim());
    }

    private static String manuscriptTitle(Manuscript m) {
        return m == null ? "" : h(m.getTitle());
    }

    private static String manuscriptCodeOrId(String manuscriptCode, Manuscript m) {
        if (manuscriptCode != null && !manuscriptCode.trim().isEmpty()) return h(manuscriptCode.trim());
        if (m != null) return String.valueOf(m.getManuscriptId());
        return "";
    }

    private static String section(String title, String innerHtml) {
        return "<div style=\"margin:14px 0;\">" +
                "<div style=\"font-weight:700;margin-bottom:6px;\">" + h(title) + "</div>" +
                "<div>" + innerHtml + "</div>" +
                "</div>";
    }

    public static MailMessage submissionConfirmation(MailConfig cfg, User author, Manuscript m, String manuscriptCode) {
        String code = manuscriptCodeOrId(manuscriptCode, m);
        String subject = "【投稿系统】稿件提交成功（已受理）- " + code;

        String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());

        String body = ""
                + "<p>尊敬的 " + person(author) + "：</p>"
                + "<p>您好！感谢您使用本投稿系统。您提交的稿件已提交成功并已受理，相关信息如下：</p>"
                + section("稿件信息",
                "<ul style=\"margin:0;padding-left:18px;\">"
                        + "<li><b>稿件编号：</b>" + h(code) + "</li>"
                        + "<li><b>稿件标题：</b>" + h(manuscriptTitle(m)) + "</li>"
                        + "</ul>")
                + (detailUrl.isEmpty()
                    ? ""
                    : section("稿件详情",
                        "<p style=\"margin:0;\">您可通过以下链接查看稿件详情与处理进度：</p>"
                        + "<p style=\"margin:6px 0 0 0;\"><a href=\"" + h(detailUrl) + "\">" + h(detailUrl) + "</a></p>"
                  ))
                + "<p>后续审稿流程的进展将通过系统站内消息及邮件方式通知您，请及时关注。</p>"
                + "<p style=\"color:#666; font-size:12px;\">（本邮件由系统自动发送，请勿直接回复。如需协助，请通过系统内“帮助/反馈”与我们联系。）</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }


    public static MailMessage unsubmitReturn(MailConfig cfg, User author, Manuscript m,
            String manuscriptCode, String issues, String guideUrl) {
		String code = manuscriptCodeOrId(manuscriptCode, m);
		String subject = "【投稿系统】形式审查退回（请修改后重新提交）- " + code;
		
		String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());
		
		String issuesHtml;
		if (issues != null && !issues.trim().isEmpty()) {
		String[] list = issues.split("[；;\n\r]+");
		StringBuilder sb = new StringBuilder();
		sb.append("<ul style=\"margin:0;padding-left:18px;\">");
		for (String it : list) {
		if (it == null) continue;
		String t = it.trim();
		if (t.isEmpty()) continue;
		sb.append("<li>").append(h(t)).append("</li>");
		}
		sb.append("</ul>");
		issuesHtml = sb.toString();
		} else {
		issuesHtml = "<p style=\"margin:0;\">编辑部未填写具体问题说明。为确保修改准确，请登录系统查看退回原因及补充材料要求。</p>";
		}
		
		
		String guideSection = "";
		if (guideUrl != null && !guideUrl.trim().isEmpty()) {
		String g = guideUrl.trim();
		guideSection = section("参考指南",
		"<p style=\"margin:0;\">请参考以下指南完成格式与材料修订：</p>"
		+ "<p style=\"margin:6px 0 0 0;\"><a href=\"" + h(g) + "\">" + h(g) + "</a></p>");
		}
		
		String body = ""
		+ "<p>尊敬的 " + person(author) + "：</p>"
		+ "<p>您好！您提交的稿件在形式审查环节未通过，现予以退回修改。请您根据以下问题及要求完成修订后，登录系统重新提交。</p>"
		+ section("稿件信息",
		"<ul style=\"margin:0;padding-left:18px;\">"
		+ "<li><b>稿件编号：</b>" + h(code) + "</li>"
		+ "<li><b>稿件标题：</b>" + h(manuscriptTitle(m)) + "</li>"
		+ "</ul>")
		+ section("需修改/补充事项", issuesHtml)
		+ guideSection
		+ (detailUrl.isEmpty() ? "" : section("查看稿件与修改入口",
		"<p style=\"margin:0;\">您可通过以下链接查看退回详情并进行修改提交：</p>"
		+ "<p style=\"margin:6px 0 0 0;\"><a href=\"" + h(detailUrl) + "\">" + h(detailUrl) + "</a></p>"))
		+ "<p>为避免影响稿件处理进度，建议您尽快完成修改并重新提交。感谢您的理解与配合！</p>"
		+ "<p style=\"color:#666; font-size:12px;\">（本邮件由系统自动发送，请勿直接回复。如需协助，请通过系统内“帮助/反馈”与编辑部取得联系。）</p>";
		
		return new MailMessage().subject(subject).htmlBody(wrap(body));
}


    
    public static MailMessage deskRejectToAuthor(MailConfig cfg, User author, Manuscript m,
            String manuscriptCode, String rejectReason) {
		String code = manuscriptCodeOrId(manuscriptCode, m);
		String subject = "【投稿系统】案头审查结果通知（退稿）- " + code;
		
		String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());
		
		String reasonHtml = (rejectReason == null || rejectReason.trim().isEmpty())
		? "<p style=\"margin:0;\">（编辑部未填写具体退稿说明，请以系统内显示为准。）</p>"
		: "<p style=\"margin:0;white-space:pre-wrap;\">" + h(rejectReason.trim()) + "</p>";
		
		String body = ""
		+ "<p>尊敬的 " + person(author) + "：</p>"
		+ "<p>您好！感谢您向本刊投稿。经编辑部主编案头审查，您的稿件未能进入外审流程，现作退稿处理，特此通知。</p>"
		+ section("稿件信息",
		"<ul style=\"margin:0;padding-left:18px;\">"
		+ "<li><b>稿件编号：</b>" + h(code) + "</li>"
		+ "<li><b>稿件标题：</b>" + h(manuscriptTitle(m)) + "</li>"
		+ "</ul>")
		+ section("退稿说明", reasonHtml)
		+ (detailUrl.isEmpty() ? "" : section("查看详情",
		"<p style=\"margin:0;\"><a href=\"" + h(detailUrl) + "\">" + h(detailUrl) + "</a></p>"))
		+ "<p>感谢您对本刊的关注与支持，欢迎您在修改完善后再次投稿。</p>"
		+ "<p style=\"color:#666; font-size:12px;\">（本邮件由系统自动发送，请勿直接回复。如需协助，请通过系统内“帮助/反馈”与编辑部取得联系。）</p>";
		
		return new MailMessage().subject(subject).htmlBody(wrap(body));
    }


    public static MailMessage reviewerInvitation(MailConfig cfg, User reviewer, Manuscript m, int reviewId, LocalDateTime dueAt) {
        String subject = "【投稿系统】审稿邀请 - 稿件 #" + m.getManuscriptId();
        String inviteUrl = link(cfg.baseUrl(), "/reviewer/invitation?id=" + reviewId);

        String dueHtml = (dueAt == null) ? "未设置" : h(fmt(dueAt));

        String body = ""
                + "<p>尊敬的 " + person(reviewer) + "：</p>"
                + "<p>您好！编辑部诚邀您为以下稿件提供匿名评审意见：</p>"
                + section("稿件信息",
                "<ul style=\"margin:0;padding-left:18px;\">" +
                        "<li><b>稿件标题：</b>" + manuscriptTitle(m) + "</li>" +
                        "<li><b>评审截止时间：</b>" + dueHtml + "</li>" +
                        "</ul>")
                + section("操作入口",
                inviteUrl.isEmpty()
                        ? "<p style=\"margin:0;\">请登录系统在“审稿邀请/待审稿件”中查看并选择接受或拒绝。</p>"
                        : "<a href=\"" + h(inviteUrl) + "\">点击查看邀请并接受/拒绝</a>")
                + section("保密提示",
                "<p style=\"margin:0;\">请对稿件内容及评审过程严格保密，仅用于学术评审目的。</p>")
                + "<p>感谢您的支持与帮助！</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage reviewerRemind(MailConfig cfg, User reviewer, Manuscript m, Review r) {
        String subject = "【投稿系统】审稿提醒 - 稿件 #" + m.getManuscriptId();
        String inviteUrl = link(cfg.baseUrl(), "/reviewer/invitation?id=" + r.getReviewId());

        String body = ""
                + "<p>尊敬的 " + person(reviewer) + "：</p>"
                + "<p>您好！现就您正在进行的评审任务向您发送提醒，请在截止时间前提交审稿意见。</p>"
                + section("稿件信息",
                "<ul style=\"margin:0;padding-left:18px;\">" +
                        "<li><b>稿件标题：</b>" + manuscriptTitle(m) + "</li>" +
                        (r.getDueAt() == null ? "" : ("<li><b>截止时间：</b>" + h(fmt(r.getDueAt())) + "</li>")) +
                        "</ul>")
                + section("提交入口",
                inviteUrl.isEmpty()
                        ? "<p style=\"margin:0;\">请登录系统在“待审稿件”中提交您的意见。</p>"
                        : "<a href=\"" + h(inviteUrl) + "\">点击进入系统提交意见</a>")
                + "<p>感谢您的支持与配合！</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage reviewerRemindCustom(MailConfig cfg,
                                                   User reviewer,
                                                   Manuscript m,
                                                   Review r,
                                                   String extraText) {
        String subject = "【投稿系统】审稿提醒 - 稿件 #" + m.getManuscriptId();
        String inviteUrl = link(cfg.baseUrl(), "/reviewer/invitation?id=" + r.getReviewId());

        String extra = (extraText != null && !extraText.trim().isEmpty())
                ? "<p style=\"margin:0;white-space:pre-wrap;\">" + h(extraText.trim()) + "</p>"
                : "<p style=\"margin:0;\">请在截止时间前完成审稿并提交意见。</p>";

        String body = ""
                + "<p>尊敬的 " + person(reviewer) + "：</p>"
                + extra
                + section("稿件信息",
                "<ul style=\"margin:0;padding-left:18px;\">" +
                        "<li><b>稿件标题：</b>" + manuscriptTitle(m) + "</li>" +
                        (r.getDueAt() == null ? "" : ("<li><b>截止时间：</b>" + h(fmt(r.getDueAt())) + "</li>")) +
                        "</ul>")
                + section("提交入口",
                inviteUrl.isEmpty()
                        ? "<p style=\"margin:0;\">请登录系统在“待审稿件”中提交您的意见。</p>"
                        : "<a href=\"" + h(inviteUrl) + "\">点击进入系统提交意见</a>")
                + "<p>感谢您的支持与配合！</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage reviewerResponseToEditor(MailConfig cfg, User editor, User reviewer, Manuscript m, boolean accepted) {
        String subject = "【投稿系统】审稿邀请回应 - " + (accepted ? "接受" : "拒绝") + "（稿件 #" + m.getManuscriptId() + "）";
        String body = ""
                + "<p>尊敬的 " + person(editor) + "：</p>"
                + "<p>您好！审稿人 <b>" + person(reviewer) + "</b>（" + h(reviewer == null ? "" : reviewer.getEmail()) + "）"
                + (accepted ? "已接受" : "已拒绝") + "对该稿件的审稿邀请。</p>"
                + section("稿件标题", "<p style=\"margin:0;\">" + manuscriptTitle(m) + "</p>");

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    
    public static MailMessage reviewerDeclinedToEditor(MailConfig cfg, User editor, User reviewer, Manuscript m, String reason) {
        String subject = "【投稿系统】审稿邀请被拒绝（稿件 #" + m.getManuscriptId() + "）";

        String reasonHtml = (reason == null || reason.trim().isEmpty())
                ? "<p style=\"margin:0;\">（未填写拒绝理由）</p>"
                : "<p style=\"margin:0;white-space:pre-wrap;\">" + h(reason.trim()) + "</p>";

        String body = ""
                + "<p>尊敬的 " + person(editor) + "：</p>"
                + "<p>您好！审稿人 <b>" + person(reviewer) + "</b> 已拒绝审稿邀请。</p>"
                + section("稿件标题", "<p style=\"margin:0;\">" + manuscriptTitle(m) + "</p>")
                + section("拒绝理由", reasonHtml)
                + "<p>建议您在系统中重新邀请其他审稿人，以免影响稿件处理进度。</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage assignEditorNotice(MailConfig cfg, User editor, User chief, Manuscript m, String chiefComment) {
        String subject = "【投稿系统】新稿件指派 - 稿件 #" + m.getManuscriptId();
        String detailUrl = link(cfg.baseUrl(), "/editor/withEditor");

        String commentHtml = (chiefComment == null || chiefComment.trim().isEmpty())
                ? ""
                : section("主编备注", "<div style=\"white-space:pre-wrap;\">" + h(chiefComment.trim()) + "</div>");

        String body = ""
                + "<p>尊敬的 " + person(editor) + "：</p>"
                + "<p>您好！主编 <b>" + person(chief) + "</b> 已将以下稿件指派由您处理：</p>"
                + section("稿件标题", "<p style=\"margin:0;\">" + manuscriptTitle(m) + "</p>")
                + commentHtml
                + section("进入系统处理",
                detailUrl.isEmpty()
                        ? "<p style=\"margin:0;\">请登录系统在“责任编辑工作台”中处理该稿件。</p>"
                        : "<a href=\"" + h(detailUrl) + "\">点击进入责任编辑工作台</a>")
                + "<p>感谢您的辛勤工作！</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage finalDecisionToAuthor(MailConfig cfg, User author, Manuscript m, String manuscriptCode, String decisionText) {
        String code = manuscriptCodeOrId(manuscriptCode, m);
        String subject = "【投稿系统】终审结果通知 - " + code;
        String detailUrl = link(cfg.baseUrl(), "/manuscripts/detail?id=" + m.getManuscriptId());

        String body = ""
                + "<p>尊敬的 " + person(author) + "：</p>"
                + "<p>您好！您的稿件已完成终审处理，结果如下：</p>"
                + section("终审结果", "<p style=\"margin:0;\"><b>" + h(decisionText) + "</b></p>")
                + section("稿件信息",
                "<ul style=\"margin:0;padding-left:18px;\">" +
                        "<li><b>稿件编号：</b>" + code + "</li>" +
                        "<li><b>稿件标题：</b>" + manuscriptTitle(m) + "</li>" +
                        "</ul>")
                + (detailUrl.isEmpty() ? "" : section("查看详情", "<a href=\"" + h(detailUrl) + "\">" + h(detailUrl) + "</a>"))
                + "<p>如需查看审稿意见汇总，请登录系统在稿件详情页面查看（如有）。</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage finalDecisionToEditor(MailConfig cfg, User editor, Manuscript m, String decisionText) {
        String subject = "【投稿系统】终审结果已发布 - 稿件 #" + m.getManuscriptId();

        String body = ""
                + "<p>尊敬的 " + person(editor) + "：</p>"
                + "<p>您好！主编已对您负责的稿件作出终审决定：</p>"
                + section("稿件标题", "<p style=\"margin:0;\">" + manuscriptTitle(m) + "</p>")
                + section("终审结果", "<p style=\"margin:0;\"><b>" + h(decisionText) + "</b></p>")
                + "<p>请留意作者后续的修改/沟通（如有）。</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage retractNoticeToAuthor(MailConfig cfg, User author, Manuscript m, String manuscriptCode) {
        String code = manuscriptCodeOrId(manuscriptCode, m);
        String subject = "【投稿系统】撤稿通知 - " + code;

        String body = ""
                + "<p>尊敬的 " + person(author) + "：</p>"
                + "<p>您好！您的稿件已被主编执行撤稿操作，系统已对该稿件进行归档处理。</p>"
                + section("稿件信息",
                "<ul style=\"margin:0;padding-left:18px;\">" +
                        "<li><b>稿件编号：</b>" + code + "</li>" +
                        "<li><b>稿件标题：</b>" + manuscriptTitle(m) + "</li>" +
                        "</ul>")
                + "<p>如对此有疑问，请通过系统联系编辑部。</p>";

        return new MailMessage().subject(subject).htmlBody(wrap(body));
    }

    public static MailMessage inviteNewReviewer(MailConfig cfg, User reviewer, String rawPassword) {
        String subject = "【投稿系统】审稿人账户邀请";
        String loginUrl = link(cfg.baseUrl(), "/auth/login");

        String pwdHtml = (rawPassword == null || rawPassword.trim().isEmpty())
                ? ""
                : "<li><b>初始密码：</b>" + h(rawPassword) + "</li>";

        String body = ""
                + "<p>尊敬的 " + person(reviewer) + "：</p>"
                + "<p>您好！编辑部邀请您担任本系统审稿人，已为您开通账号。请妥善保管登录信息并尽快修改初始密码。</p>"
                + section("账户信息",
                "<ul style=\"margin:0;padding-left:18px;\">" +
                        "<li><b>用户名：</b>" + h(reviewer == null ? "" : reviewer.getUsername()) + "</li>" +
                        pwdHtml +
                        "</ul>")
                + section("登录入口",
                loginUrl.isEmpty()
                        ? "<p style=\"margin:0;\">请登录系统入口页面进行登录。</p>"
                        : "<a href=\"" + h(loginUrl) + "\">点击进入登录页面</a>")
                + "<p>感谢您的支持与帮助！</p>";

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
        return "<html><body style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;background:#fafafa;padding:18px;\">"
                + "<div style=\"max-width:760px;margin:0 auto;background:#fff;border:1px solid #eee;border-radius:10px;padding:22px;\">"
                + "<div style=\"font-size:18px;font-weight:700;margin-bottom:10px;\">稿件投稿系统通知</div>"
                + inner
                + "<hr style=\"margin-top:22px;border:none;border-top:1px solid #eee;\"/>"
                + "<div style=\"color:#6c757d;font-size:12px;line-height:1.6;\">"
                + "本邮件由系统自动发送，请勿直接回复。若需联系编辑部，请通过系统“消息中心/联系我们”提交咨询。"
                + "</div>"
                + "</div></body></html>";
    }

    private static String link(String baseUrl, String path) {
        if (baseUrl == null) return "";
        String b = baseUrl.trim();
        if (b.isEmpty()) return "";
        if (!b.startsWith("http://") && !b.startsWith("https://")) {
            return ""; 
        }
        if (path == null) path = "";
        if (!path.startsWith("/")) path = "/" + path;
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + path;
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

