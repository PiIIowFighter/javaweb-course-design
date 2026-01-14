package edu.bjfu.onlinesm.util.notify;

import edu.bjfu.onlinesm.dao.*;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;


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
    
    public void onFormalCheckStarted(int manuscriptId) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            String title = "形式审查已开始";
            String content = "编辑部已开始对您的稿件进行形式审查，请耐心等待处理结果。";
            if (m.getTitle() != null) content += "\n稿件标题：" + safe(m.getTitle());
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "FORMAL_CHECK", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    
    public void onFormalCheckPassed(int manuscriptId) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            String title = "形式审查已通过";
            String content = "您的稿件已通过形式审查，已进入编辑部案头初审阶段。";
            if (m.getTitle() != null) content += "\n稿件标题：" + safe(m.getTitle());
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "FORMAL_CHECK", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    
    public void onDeskAccepted(int manuscriptId) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            String title = "案头初审通过";
            String content = "您的稿件已通过主编案头初审，编辑部将为您分配责任编辑并进入外审流程。";
            if (m.getTitle() != null) content += "\n稿件标题：" + safe(m.getTitle());
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "DESK_REVIEW", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    
    public void onDeskRejected(int manuscriptId, String rejectReason) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            String title = "案头退稿通知";
            String content = "很遗憾，您的稿件未通过主编案头初审，已做退稿处理。";
            if (m.getTitle() != null) content += "\n稿件标题：" + safe(m.getTitle());
            if (rejectReason != null && !rejectReason.trim().isEmpty()) {
                content += "\n退稿理由：" + rejectReason.trim();
            }
            notificationDAO.create(author.getUserId(), null, "SYSTEM", "DESK_REVIEW", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    
    public void onEditorAssignedToAuthor(int manuscriptId, User chief, int editorId) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;
            User author = userDAO.findById(m.getSubmitterId());
            if (author == null) return;
            User editor = userDAO.findById(editorId);

            String title = "稿件已分配责任编辑";
            String content = "您的稿件已分配责任编辑处理，后续流程将由责任编辑推进。";
            if (m.getTitle() != null) content += "\n稿件标题：" + safe(m.getTitle());
            if (editor != null) {
                String editorName = editor.getFullName() != null ? editor.getFullName() : editor.getUsername();
                content += "\n责任编辑：" + safe(editorName);
            }
            notificationDAO.create(author.getUserId(), chief == null ? null : chief.getUserId(), "SYSTEM", "ASSIGN", title, content, manuscriptId);
        } catch (Exception ignore) {
        }
    }

    
    public void onReviewSubmitted(int reviewId) {
        try {
            Review r = reviewDAO.findById(reviewId);
            if (r == null) return;

            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            Integer editorId = manuscriptDAO.findCurrentEditorId(r.getManuscriptId());
            if (editorId == null) return;

            User editor = userDAO.findById(editorId);
            if (editor == null) return;

            User reviewer = userDAO.findById(r.getReviewerId());

            String title = "收到新的外审意见";
            String content = "一位审稿人已提交外审意见，请及时查看并推进后续处理。";
            if (m != null && m.getTitle() != null) content += "\n稿件标题：" + safe(m.getTitle());
            if (reviewer != null) content += "\n审稿人：" + safe(reviewer.getUsername());
            if (r.getRecommendation() != null && !r.getRecommendation().trim().isEmpty()) {
                content += "\n推荐结论：" + safe(r.getRecommendation().trim());
            }

            notificationDAO.create(editor.getUserId(), null, "SYSTEM", "REVIEW_SUBMITTED", title, content, r.getManuscriptId());
        } catch (Exception ignore) {
        }
    }

    

    
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

    
    public void onReviewerResponded(int reviewId, boolean accepted) {
        onReviewerResponded(reviewId, accepted, null);
    }

    
    public void onReviewerResponded(int reviewId, boolean accepted, String rejectionReasonOverride) {
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

            
            if (!accepted) {
                String reason = (rejectionReasonOverride != null ? rejectionReasonOverride : r.getRejectionReason());
                if (reason != null) reason = reason.trim();
                if (reason != null && !reason.isEmpty()) {
                    content += "\n拒绝理由：" + reason;
                }
            }
            notificationDAO.create(editor.getUserId(), null, "SYSTEM", "REVIEW_RESPONSE", title, content, r.getManuscriptId());
        } catch (Exception ignore) {
        }
    }

    
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


    
    public void onEditorRecommendationSubmitted(int manuscriptId, User editor, String suggestionText, String summary) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            String title = "收到编辑建议（待主编终审）";
            String content = "编辑已提交处理建议，请前往终审列表查看并作出最终决策。";
            if (m != null) content += "\n稿件标题：" + safe(m.getTitle());
            if (editor != null) content += "\n提交人：" + safe(editor.getFullName() != null ? editor.getFullName() : editor.getUsername());
            if (suggestionText != null && !suggestionText.trim().isEmpty()) content += "\n建议：" + suggestionText.trim();
            if (summary != null && !summary.trim().isEmpty()) content += "\n总结：" + summary.trim();

            
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

    
    public void onFinalDecision(int manuscriptId, String decisionText) {
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) return;

            
            User author = userDAO.findById(m.getSubmitterId());
            if (author != null) {
                String title = "终审结果已出";
                String content = "您的稿件终审结果已更新：" + (decisionText == null ? "" : decisionText) + "。";
                notificationDAO.create(author.getUserId(), null, "SYSTEM", "FINAL_DECISION", title, content, manuscriptId);
            }

            
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

    
    public void onInviteNewReviewer(User reviewer) {
        try {
            if (reviewer == null || reviewer.getUserId() == null) return;
            String title = "审稿人账号已创建";
            String content = "您已被邀请成为审稿人，请使用系统分配的账号登录。";
            notificationDAO.create(reviewer.getUserId(), null, "SYSTEM", "REVIEWER_INVITE", title, content, null);
        } catch (Exception ignore) {
        }
    }


    
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

