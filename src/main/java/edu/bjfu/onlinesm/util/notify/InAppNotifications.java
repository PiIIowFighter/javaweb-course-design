package edu.bjfu.onlinesm.util.notify;

import edu.bjfu.onlinesm.dao.*;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;

/**
 * 统一封装“业务事件 -> 站内通知”。
 *
 * 仅做单向通知，不做对话串。
 * 通知写入失败不影响主流程。
 */
public class InAppNotifications {

    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final ManuscriptDAO manuscriptDAO;
    private final ReviewDAO reviewDAO;

    public InAppNotifications(UserDAO userDAO, ManuscriptDAO manuscriptDAO, ReviewDAO reviewDAO) {
        this.notificationDAO = new NotificationDAO();
        this.userDAO = userDAO;
        this.manuscriptDAO = manuscriptDAO;
        this.reviewDAO = reviewDAO;
    }

    /** 投稿提交成功：给作者发通知。 */
    public void onSubmissionSuccess(User author, Manuscript m, String manuscriptCode) {
        try {
            if (author == null || author.getUserId() == null) return;
            String title = "投稿提交成功";
            String content = "您的稿件已成功提交，稿件编号：" + manuscriptCode + "。";
            Integer mid = (m == null ? null : m.getManuscriptId());
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "SUBMISSION", title, content, mid);
        } catch (Exception ignore) {
        }
    }

    /** 形式审查退回：给作者发通知。 */
    public void onFormalCheckReturn(int manuscriptId, String issues) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            String title = "稿件被退回修改";
            String content = "您的稿件未通过形式审查，需要修改后重新提交。" + (issues == null || issues.trim().isEmpty() ? "" : ("\n问题：" + issues));
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "FORMAL_CHECK", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    /** 邀请审稿人：给审稿人发通知。 */
    public void onReviewerInvited(int reviewId) {
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            User reviewer = userDAO.findById(r.getReviewerId());
            if (reviewer == null) return;
            String title = "收到审稿邀请";
            String content = "您收到了新的审稿邀请。" + (m == null ? "" : ("稿件标题：" + safe(m.getTitle()))) + "\n请在系统中查看详情并接受/拒绝邀请。";
            notificationDAO.create(reviewer.getUserId(), null, "SYSTEM", "REVIEW_INVITE", title, content, r.getManuscriptId());
        } catch (Exception ignore) {
        }
    }

    /** 催审提醒：给审稿人发通知。 */
    public void onReviewerRemind(int reviewId) {
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            User reviewer = userDAO.findById(r.getReviewerId());
            if (reviewer == null) return;
            String title = "审稿提醒";
            String content = "请尽快完成审稿并提交意见。" + (m == null ? "" : ("\n稿件标题：" + safe(m.getTitle())));
            notificationDAO.create(reviewer.getUserId(), null, "SYSTEM", "REVIEW_REMIND", title, content, r.getManuscriptId());
        } catch (Exception ignore) {
        }
    }

    /** 审稿人接受/拒绝：通知编辑（主责编辑）。 */
    public void onReviewerResponded(int reviewId, boolean accepted) {
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            User reviewer = userDAO.findById(r.getReviewerId());
            Integer editorId = manuscriptDAO.findCurrentEditorId(r.getManuscriptId());
            if (editorId == null) return;
            User editor = userDAO.findById(editorId);
            if (editor == null) return;

            String title = accepted ? "审稿邀请已接受" : "审稿邀请被拒绝";
            String content = "审稿人：" + (reviewer == null ? "" : safe(reviewer.getUsername())) + (accepted ? " 已接受" : " 已拒绝") + "审稿邀请。";
            if (m != null) content += "\n稿件标题：" + safe(m.getTitle());
            notificationDAO.create(editor.getUserId(), null, "SYSTEM", "REVIEW_RESPONSE", title, content, r.getManuscriptId());
        } catch (Exception ignore) {
        }
    }

    /** 主编指派编辑：通知被指派编辑。 */
    public void onEditorAssigned(int manuscriptId, User chief, int editorId, String chiefComment) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            User editor = userDAO.findById(editorId);
            if (editor == null) return;
            String title = "收到新稿件处理任务";
            String content = "主编已将稿件指派给您处理。";
            if (m != null) content += "\n稿件标题：" + safe(m.getTitle());
            if (chiefComment != null && !chiefComment.trim().isEmpty()) content += "\n主编备注：" + chiefComment.trim();
            notificationDAO.create(editor.getUserId(), chief == null ? null : chief.getUserId(), "SYSTEM", "ASSIGN", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }


    /** 编辑提交建议：通知主编（终审队列）。 */
    public void onEditorRecommendationSubmitted(int manuscriptId, User editor, String suggestionText, String summary) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            String title = "收到编辑建议（待主编终审）";
            String content = "编辑已提交处理建议，请前往终审列表查看并作出最终决策。";
            if (m != null) content += "\n稿件标题：" + safe(m.getTitle());
            if (editor != null) content += "\n提交人：" + safe(editor.getFullName() != null ? editor.getFullName() : editor.getUsername());
            if (suggestionText != null && !suggestionText.trim().isEmpty()) content += "\n建议：" + suggestionText.trim();
            if (summary != null && !summary.trim().isEmpty()) content += "\n总结：" + summary.trim();

            // 通知所有主编（支持多主编）
            for (User chief : userDAO.findByRoleCode("EDITOR_IN_CHIEF")) {
                notificationDAO.create(chief.getUserId(),
                        editor == null ? null : editor.getUserId(),
                        "SYSTEM",
                        "EDITOR_RECOMMENDATION",
                        title,
                        content,
                        manuscriptId);
            }
        } catch (Exception ignore) {
        }
    }

    /** 终审决策：通知作者与编辑。 */
    public void onFinalDecision(int manuscriptId, String decisionText) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;

            // 作者
            User author = userDAO.findById(m.getSubmitterId());
            if (author != null) {
                String title = "终审结果已出";
                String content = "您的稿件终审结果已更新：" + (decisionText == null ? "" : decisionText) + "。";
                notificationDAO.create(author.getUserId(), null, "SYSTEM", "FINAL_DECISION", title, content, manuscriptId);
            }

            // 编辑
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (editorId != null) {
                User editor = userDAO.findById(editorId);
                if (editor != null) {
                    String title = "稿件终审结果已更新";
                    String content = "主编已对稿件做出终审决定：" + (decisionText == null ? "" : decisionText) + "。";
                    if (m != null) content += "\n稿件标题：" + safe(m.getTitle());
                    notificationDAO.create(editor.getUserId(), null, "SYSTEM", "FINAL_DECISION", title, content, manuscriptId);
                }
            }
        } catch (Exception ignore) {
        }
    }

    /** 撤稿：通知作者。 */
    public void onRetract(int manuscriptId) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            String title = "稿件已撤稿";
            String content = "您的稿件已被撤稿。" + (m.getTitle() == null ? "" : ("\n稿件标题：" + safe(m.getTitle())));
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "RETRACT", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    /** 主编邀请新审稿人（创建账号）：给被邀请人发一条站内通知。 */
    public void onInviteNewReviewer(User reviewer) {
        try {
            if (reviewer == null || reviewer.getUserId() == null) return;
            String title = "审稿人账号已创建";
            String content = "您已被邀请成为审稿人，请使用系统分配的账号登录。";
            notificationDAO.create(reviewer.getUserId(), null, "SYSTEM", "REVIEWER_INVITE", title, content, null);
        } catch (Exception ignore) {
        }
    }


    /**
     * 催办责任编辑：在站内向该编辑发送一条“处理稿件提醒”通知。
     */
    public void onEditorReminder(Manuscript manuscript, User chief, User editor) {
        try {
            if (manuscript == null || editor == null || editor.getUserId() == null) return;
            String code = "MS" + String.format("%05d", manuscript.getManuscriptId());
            String title = "请尽快处理稿件 " + code;

            StringBuilder content = new StringBuilder();
            content.append("主编/编辑部管理员 ");
            if (chief != null) {
                content.append(safe(chief.getFullName()));
            }
            content.append(" 提醒您尽快处理稿件：");
            content.append("\n标题：").append(safe(manuscript.getTitle()));

            notificationDAO.create(
                    editor.getUserId(),
                    null,
                    "SYSTEM",
                    "EDITOR_REMIND",
                    title,
                    content.toString(),
                    null
            );
        } catch (Exception ignore) {
        }
    }


    private String safe(String s) {
        return s == null ? "" : s;
    }
}
