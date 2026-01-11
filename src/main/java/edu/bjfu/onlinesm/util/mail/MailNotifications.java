package edu.bjfu.onlinesm.util.mail;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.UploadPathUtil;
import java.io.IOException;
import java.util.List;

/**
 * 统一封装"业务事件 -> 发邮件"。
 *
 * 注意：邮件失败不应影响主流程（避免事务成功但邮件异常导致操作失败）。
 */
public class MailNotifications {

    private final MailConfig cfg;
    private final SmtpMailer mailer;
    private final UserDAO userDAO;
    private final ManuscriptDAO manuscriptDAO;
    private final ReviewDAO reviewDAO;

    public MailNotifications(UserDAO userDAO, ManuscriptDAO manuscriptDAO, ReviewDAO reviewDAO) {
        this.cfg = MailConfig.load();
        this.mailer = new SmtpMailer(cfg);
        this.userDAO = userDAO;
        this.manuscriptDAO = manuscriptDAO;
        this.reviewDAO = reviewDAO;
    }

    /** 投稿提交成功：给作者发"投稿确认邮件"。 */
    public void onSubmissionSuccess(User author, Manuscript m, String manuscriptCode) {
        if (!cfg.enabled()) return;
        if (author == null || author.getEmail() == null || author.getEmail().trim().isEmpty()) return;
        try {
            MailMessage msg = MailTemplates.submissionConfirmation(cfg, author, m, manuscriptCode)
                    .addTo(author.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("投稿确认邮件发送失败", e);
        }
    }

    /** 形式审查不通过退回（Unsubmit）：给作者发"退回修改邮件"。 */
    public void onFormalCheckReturn(int manuscriptId, String issues, String guideUrl) {
        if (!cfg.enabled()) return;
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null || author.getEmail() == null || author.getEmail().trim().isEmpty()) return;
            String code = manuscriptCode(manuscriptId);
            MailMessage msg = MailTemplates.unsubmitReturn(cfg, author, m, code, issues, guideUrl)
                    .addTo(author.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("退回修改邮件发送失败", e);
        }
    }
    /** 主编案头退稿：通知作者（含退稿理由）。 */
    public void onDeskRejected(int manuscriptId, String rejectReason) {
        if (!cfg.enabled()) return;
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null || author.getEmail() == null || author.getEmail().trim().isEmpty()) return;

            String code = edu.bjfu.onlinesm.util.ManuscriptCodeUtil.code(manuscriptId);

            MailMessage msg = MailTemplates.deskRejectToAuthor(cfg, author, m, code, rejectReason)
                    .addTo(author.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("案头退稿邮件发送失败", e);
        }
    }

    

    /** 编辑邀请审稿人：给审稿人发"审稿邀请邮件"。 */
    public void onReviewerInvited(int reviewId) {
        if (!cfg.enabled()) return;
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            if (m == null) return;
            User reviewer = userDAO.findById(r.getReviewerId());
            if (reviewer == null || reviewer.getEmail() == null || reviewer.getEmail().trim().isEmpty()) return;
            MailMessage msg = MailTemplates.reviewerInvitation(cfg, reviewer, m, reviewId, r.getDueAt())
                    .addTo(reviewer.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("审稿邀请邮件发送失败", e);
        }
    }

    /** 催审：给审稿人发"催审邮件"。 */
    public void onReviewerRemind(int reviewId) {
        if (!cfg.enabled()) return;
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            if (m == null) return;
            User reviewer = userDAO.findById(r.getReviewerId());
            if (reviewer == null || reviewer.getEmail() == null || reviewer.getEmail().trim().isEmpty()) return;
            MailMessage msg = MailTemplates.reviewerRemind(cfg, reviewer, m, r)
                    .addTo(reviewer.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("催审邮件发送失败", e);
        }
    }


    /**
     * 自定义内容的催审邮件（审稿监控页面中使用）。
     */
    public void onReviewerRemindCustom(int reviewId, String extraText) {
        if (!cfg.enabled()) return;
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            if (m == null) return;
            User reviewer = userDAO.findById(r.getReviewerId());
            if (reviewer == null || reviewer.getEmail() == null || reviewer.getEmail().trim().isEmpty()) return;

            MailMessage msg = MailTemplates.reviewerRemindCustom(cfg, reviewer, m, r, extraText)
                    .addTo(reviewer.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("催审邮件发送失败（自定义模板）", e);
        }
    }

    /** 审稿人拒绝邀请：通知编辑。 */
    public void onReviewerDeclined(int reviewId) {
        onReviewerDeclined(reviewId, null);
    }

    /**
     * 审稿人拒绝邀请：通知编辑（带拒绝理由覆盖）。
     * 说明：为兼容旧库（缺少 RejectionReason 列）场景，可以在调用处直接传入拒绝理由，确保邮件中可见。
     */
    public void onReviewerDeclined(int reviewId, String rejectionReasonOverride) {
        if (!cfg.enabled()) return;
        try {
            Review review = reviewDAO.findById(reviewId);
            if (review == null) return;

            Manuscript manuscript = manuscriptDAO.findById(review.getManuscriptId());
            if (manuscript == null) return;

            // 找到负责编辑
            User editor = null;
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscript.getManuscriptId());
            if (editorId != null) editor = userDAO.findById(editorId);
            if (editor == null) editor = findEditorForManuscript(manuscript.getManuscriptId());

            if (editor == null || editor.getEmail() == null || editor.getEmail().trim().isEmpty()) return;

            User reviewer = userDAO.findById(review.getReviewerId());

            String reason = rejectionReasonOverride;
            if (reason == null || reason.trim().isEmpty()) reason = review.getRejectionReason();

            MailMessage msg = MailTemplates.reviewerDeclinedToEditor(cfg, editor, reviewer, manuscript, reason)
                    .addTo(editor.getEmail());

            mailer.send(msg);
        } catch (Exception e) {
            log("审稿人拒绝邀请邮件发送失败", e);
        }
    }

    /**
     * 查找稿件的编辑（如果稿件没有指定编辑，则查找系统管理员或任何编辑）
     （如果稿件没有指定编辑，则查找系统管理员或任何编辑）
     */
    private User findEditorForManuscript(int manuscriptId) {
        try {
            // 首先尝试从ManuscriptDAO获取当前编辑
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (editorId != null) {
                User editor = userDAO.findById(editorId);
                if (editor != null && editor.getEmail() != null && !editor.getEmail().trim().isEmpty()) {
                    return editor;
                }
            }
            
            // 如果没有找到，则查找任何具有编辑或管理员角色的用户
            String sql = "SELECT TOP 1 * FROM dbo.Users WHERE RoleCode IN ('EDITOR', 'ADMIN') AND Email IS NOT NULL ORDER BY RoleCode";
            // 注意：这里我们假设UserDAO有执行自定义查询的方法，如果没有，可能需要添加
            // 由于UserDAO可能没有这个方法，我们简化处理：直接返回第一个编辑
            List<User> allUsers = userDAO.findAll();
            for (User user : allUsers) {
                if (("EDITOR".equals(user.getRoleCode()) || "ADMIN".equals(user.getRoleCode())) 
                    && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("[MailNotifications] Failed to find editor for manuscript: " + e.getMessage());
        }
        return null;
    }

    /** 审稿人接受/拒绝邀请：通知编辑。 */
    public void onReviewerResponded(int reviewId, boolean accepted) {
        if (!cfg.enabled()) return;
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            if (m == null) return;
            User reviewer = userDAO.findById(r.getReviewerId());
            if (reviewer == null) return;

            Integer editorId = manuscriptDAO.findCurrentEditorId(m.getManuscriptId());
            if (editorId == null) return;
            User editor = userDAO.findById(editorId);
            if (editor == null || editor.getEmail() == null || editor.getEmail().trim().isEmpty()) return;

            MailMessage msg = MailTemplates.reviewerResponseToEditor(cfg, editor, reviewer, m, accepted)
                    .addTo(editor.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("审稿人回应通知邮件发送失败", e);
        }
    }

    /** 主编指派编辑：通知被指派编辑。 */
    public void onEditorAssigned(int manuscriptId, User chief, int editorId, String chiefComment) {
        if (!cfg.enabled()) return;
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User editor = userDAO.findById(editorId);
            if (editor == null || editor.getEmail() == null || editor.getEmail().trim().isEmpty()) return;
            MailMessage msg = MailTemplates.assignEditorNotice(cfg, editor, chief, m, chiefComment)
                    .addTo(editor.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("指派编辑通知邮件发送失败", e);
        }
    }

    /** 主编终审决策：通知作者（并抄送编辑），并附上审稿意见附件（汇总）。 */
    public void onFinalDecision(int manuscriptId, String decisionText) {
        if (!cfg.enabled()) return;
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null || author.getEmail() == null || author.getEmail().trim().isEmpty()) return;

            // 汇总审稿意见作为附件
            List<Review> all = reviewDAO.findByManuscript(manuscriptId);
            byte[] summary = MailTemplates.buildReviewSummaryTxt(all);
            MailAttachment attach = new MailAttachment("review_summary_" + manuscriptId + ".txt", "text/plain; charset=UTF-8", summary);

            String code = manuscriptCode(manuscriptId);
            MailMessage toAuthor = MailTemplates.finalDecisionToAuthor(cfg, author, m, code, decisionText)
                    .addTo(author.getEmail())
                    .addAttachment(attach);

            // 同时给编辑（如果存在）
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (editorId != null) {
                User editor = userDAO.findById(editorId);
                if (editor != null && editor.getEmail() != null && !editor.getEmail().trim().isEmpty()) {
                    toAuthor.addCc(editor.getEmail());
                }
            }

            mailer.send(toAuthor);

            // 若需要给编辑单独一封（更符合"通知编辑"），这里再发一封（可选）
            Integer editorId2 = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (editorId2 != null) {
                User editor = userDAO.findById(editorId2);
                if (editor != null && editor.getEmail() != null && !editor.getEmail().trim().isEmpty()) {
                    MailMessage toEditor = MailTemplates.finalDecisionToEditor(cfg, editor, m, decisionText)
                            .addTo(editor.getEmail())
                            .addAttachment(attach);
                    mailer.send(toEditor);
                }
            }

        } catch (Exception e) {
            log("终审结果邮件发送失败", e);
        }
    }

    /** 撤稿：通知作者。 */
    public void onRetract(int manuscriptId) {
        if (!cfg.enabled()) return;
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null || author.getEmail() == null || author.getEmail().trim().isEmpty()) return;
            String code = manuscriptCode(manuscriptId);
            MailMessage msg = MailTemplates.retractNoticeToAuthor(cfg, author, m, code)
                    .addTo(author.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("撤稿通知邮件发送失败", e);
        }
    }

    /** 主编邀请新审稿人：发送"新审稿人邀请邮件"。 */
    public void onInviteNewReviewer(User reviewer, String rawPassword) {
        if (!cfg.enabled()) return;
        if (reviewer == null || reviewer.getEmail() == null || reviewer.getEmail().trim().isEmpty()) return;
        try {
            MailMessage msg = MailTemplates.inviteNewReviewer(cfg, reviewer, rawPassword)
                    .addTo(reviewer.getEmail());
            mailer.send(msg);
        } catch (Exception e) {
            log("新审稿人邀请邮件发送失败", e);
        }
    }

    private String manuscriptCode(int manuscriptId) {
        return "MS" + String.format("%05d", manuscriptId);
    }

    private void log(String msg, Exception e) {
        System.err.println("[MAIL] " + msg + ": " + (e == null ? "" : e.getMessage()));
        if (cfg.debug() && e != null) {
            e.printStackTrace();
        }
    }
   
}