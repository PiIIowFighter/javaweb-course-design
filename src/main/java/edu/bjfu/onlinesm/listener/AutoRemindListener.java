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


@WebListener
public class AutoRemindListener implements ServletContextListener {

    private Timer timer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
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

        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Review> targets = reviewDAO.findOverdueForAutoRemind(overdueDays, minIntervalDays, maxPerRun);
                    if (targets == null || targets.isEmpty()) return;
                    System.out.println("[AutoRemind] found " + targets.size() + " overdue reviews.");

                    for (Review r : targets) {
                        try {
                            
                            notifications.onReviewerRemind(r.getReviewId());

                            
                            reviewDAO.remind(r.getReviewId());

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

