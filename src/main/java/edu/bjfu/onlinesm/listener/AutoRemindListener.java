package edu.bjfu.onlinesm.listener;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.util.mail.MailConfig;
import edu.bjfu.onlinesm.util.mail.MailNotifications;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 自动催审监听器：
 * - Tomcat 启动后定时扫描逾期审稿任务
 * - 对逾期审稿人发送催审邮件
 *
 * 注意：
 * - 邮件发送失败不会影响主业务流程（仅打印日志）
 * - 为避免“没发出去但记录已提醒”，逻辑是：先发邮件，成功后再更新提醒时间/次数
 */
@WebListener
public class AutoRemindListener implements ServletContextListener {

    private Timer timer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 统一使用 util.mail 下的配置与发送实现
        MailConfig cfg = MailConfig.load();
        if (!cfg.enabled()) {
            System.out.println("[AutoRemind] mail disabled, job not started.");
            return;
        }

        ReviewDAO reviewDAO = new ReviewDAO();
        MailNotifications notifications = new MailNotifications(new UserDAO(), new ManuscriptDAO(), reviewDAO);

        int overdueDays = cfg.getAutoRemindOverdueDays();
        int minIntervalDays = cfg.getAutoRemindMinIntervalDays();
        int maxPerRun = cfg.getAutoRemindMaxPerRun();

        timer = new Timer("auto-remind-timer", true);

        // 每天跑一次：启动后 60 秒第一次执行
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Review> targets = reviewDAO.findOverdueForAutoRemind(overdueDays, minIntervalDays, maxPerRun);
                    if (targets == null || targets.isEmpty()) return;
                    System.out.println("[AutoRemind] found " + targets.size() + " overdue reviews.");

                    for (Review r : targets) {
                        try {
                            // 1) 先发邮件：成功才算“已提醒”
                            notifications.onReviewerRemind(r.getReviewId());

                            // 2) 发成功后再更新提醒次数/时间
                            reviewDAO.remindChecked(r.getReviewId());

                        } catch (Exception e) {
                            System.out.println("[AutoRemind] remind failed reviewId=" + r.getReviewId() + ": " + e.getMessage());
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("[AutoRemind] db error: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("[AutoRemind] unexpected error: " + e.getMessage());
                }
            }
        }, 60_000L, 24L * 60L * 60L * 1000L);

        System.out.println("[AutoRemind] job started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
