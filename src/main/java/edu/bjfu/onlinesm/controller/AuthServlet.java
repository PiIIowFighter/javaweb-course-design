package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.MenuPermissionService;
import edu.bjfu.onlinesm.util.mail.MailService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;


@WebServlet(name = "AuthServlet", urlPatterns = {"/auth/*"})
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final MenuPermissionService menuPermissionService = new MenuPermissionService();

    
    private static final String SESSION_REG_OTP_CODE = "REG_OTP_CODE";
    private static final String SESSION_REG_OTP_EMAIL = "REG_OTP_EMAIL";
    private static final String SESSION_REG_OTP_EXPIRES_AT = "REG_OTP_EXPIRES_AT"; 

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/login";
        }

        switch (path) {
            case "/login":
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                break;
            case "/register":
                req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                break;
            case "/reset":
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                break;
            case "/logout":
                HttpSession session = req.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                resp.sendRedirect(req.getContextPath() + "/");
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/login";
        }

        switch (path) {
            case "/login":
                handleLogin(req, resp);
                break;
            case "/register":
                handleRegister(req, resp);
                break;
            case "/reset":
                handleReset(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = trim(req.getParameter("username"));
        String password = trim(req.getParameter("password"));

        if (isEmpty(username) || isEmpty(password)) {
            req.setAttribute("error", "用户名和密码均不能为空。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        try {
            User user = userDAO.findByUsername(username);
            if (user == null || user.getPasswordHash() == null ||
                    !user.getPasswordHash().equals(password)) {
                req.setAttribute("error", "用户名或密码错误。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                return;
            }

            String status = user.getStatus();
            if (status != null && !"ACTIVE".equalsIgnoreCase(status)) {
                req.setAttribute("error", "账号状态为 " + status + "，无法登录，请联系管理员。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                return;
            }

            
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", user);
            
            menuPermissionService.loadIntoSession(session, user);
            
            resp.sendRedirect(req.getContextPath() + "/");
        } catch (SQLException e) {
            throw new ServletException("登录时访问数据库出错", e);
        }
    }

    
    
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        String op = trim(req.getParameter("op"));
        if ("sendCode".equalsIgnoreCase(op)) {
            handleSendRegisterCode(req, resp);
            return;
        }

        String username = trim(req.getParameter("username"));
        String password = trim(req.getParameter("password"));
        String confirmPassword = trim(req.getParameter("confirmPassword"));
        String email = trim(req.getParameter("email"));
        String emailCode = trim(req.getParameter("emailCode"));
        String fullName = trim(req.getParameter("fullName"));
        String affiliation = trim(req.getParameter("affiliation"));
        String researchArea = trim(req.getParameter("researchArea"));
        String registerRole = trim(req.getParameter("registerRole")); 

        if (isEmpty(username) || isEmpty(password) || isEmpty(confirmPassword)) {
            req.setAttribute("error", "用户名、密码和确认密码均不能为空。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (password.length() < 8) {
            req.setAttribute("error", "密码长度至少为 8 位，请重新输入。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "两次输入的密码不一致。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        
        if (isEmpty(email)) {
            req.setAttribute("error", "邮箱不能为空。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (isEmpty(emailCode)) {
            req.setAttribute("error", "请先点击“发送验证码”，并在下方填写收到的验证码。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (!verifyRegisterEmailOtp(req.getSession(false), email, emailCode)) {
            req.setAttribute("error", "邮箱验证码无效或已过期，请重新发送验证码后再注册。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        
        String targetRoleCode = "AUTHOR";
        if ("REVIEWER".equalsIgnoreCase(registerRole)) {
            targetRoleCode = "REVIEWER";
        }

        try {
            if (userDAO.findByUsername(username) != null) {
                
                req.setAttribute("error", "用户已存在，请重新注册");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                return;
            }

            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(password); 
            user.setEmail(defaultString(email));
            user.setFullName(defaultString(fullName));
            user.setAffiliation(defaultString(affiliation));
            user.setResearchArea(defaultString(researchArea));
            
            user.setStatus("ACTIVE");

            if ("REVIEWER".equals(targetRoleCode)) {
                userDAO.createUserWithRole(user, "REVIEWER");
            } else {
                userDAO.registerAuthor(user);
            }

            
            clearRegisterEmailOtp(req.getSession(false));

            
            req.setAttribute("message", "注册成功，您的账户已激活，可直接登录。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("注册时访问数据库出错", e);
        }
    }

    
    private void handleSendRegisterCode(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = trim(req.getParameter("email"));
        if (isEmpty(email)) {
            req.setAttribute("error", "请先填写邮箱后再发送验证码。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        long expiresAt = System.currentTimeMillis() + 5L * 60L * 1000L;

        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_REG_OTP_EMAIL, email);
        session.setAttribute(SESSION_REG_OTP_CODE, code);
        session.setAttribute(SESSION_REG_OTP_EXPIRES_AT, expiresAt);

        
        String subject = "注册验证码";
        String body = "您的注册验证码为：" + code + "\n\n有效期 5 分钟。若非本人操作请忽略此邮件。";
        MailService.sendText(email, subject, body);

        req.setAttribute("message", "验证码已发送，请在 5 分钟内查收邮箱并填写下方验证码。");
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }

    private boolean verifyRegisterEmailOtp(HttpSession session, String email, String code) {
        if (session == null) return false;
        Object se = session.getAttribute(SESSION_REG_OTP_EMAIL);
        Object sc = session.getAttribute(SESSION_REG_OTP_CODE);
        Object sx = session.getAttribute(SESSION_REG_OTP_EXPIRES_AT);

        if (!(se instanceof String) || !(sc instanceof String) || sx == null) return false;
        String storedEmail = ((String) se).trim();
        String storedCode = ((String) sc).trim();

        long expiresAt;
        try {
            if (sx instanceof Long) {
                expiresAt = (Long) sx;
            } else {
                expiresAt = Long.parseLong(String.valueOf(sx));
            }
        } catch (Exception ignore) {
            return false;
        }

        if (System.currentTimeMillis() > expiresAt) return false;
        if (email == null || !storedEmail.equalsIgnoreCase(email.trim())) return false;
        return storedCode.equals(code == null ? "" : code.trim());
    }

    private void clearRegisterEmailOtp(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(SESSION_REG_OTP_EMAIL);
        session.removeAttribute(SESSION_REG_OTP_CODE);
        session.removeAttribute(SESSION_REG_OTP_EXPIRES_AT);
    }

    
    
    private void handleReset(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String op = trim(req.getParameter("op"));
        HttpSession session = req.getSession(true);

        final String SESSION_RESET_USERNAME = "RESET_OTP_USERNAME";
        final String SESSION_RESET_EMAIL = "RESET_OTP_EMAIL";
        final String SESSION_RESET_CODE = "RESET_OTP_CODE";
        final String SESSION_RESET_EXPIRES_AT = "RESET_OTP_EXPIRES_AT";
        final String SESSION_RESET_VERIFIED = "RESET_VERIFIED_USERNAME";

        if ("sendResetCode".equalsIgnoreCase(op)) {
            String username = trim(req.getParameter("username"));
            if (isEmpty(username)) {
                req.setAttribute("error", "请先填写用户名。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            try {
                User user = userDAO.findByUsername(username);
                if (user == null || isEmpty(user.getEmail())) {
                    req.setAttribute("error", "未找到该用户或该用户未绑定邮箱。");
                    req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                    return;
                }

                String email = user.getEmail().trim();
                String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
                long expiresAt = System.currentTimeMillis() + 5L * 60L * 1000L;

                session.setAttribute(SESSION_RESET_USERNAME, username);
                session.setAttribute(SESSION_RESET_EMAIL, email);
                session.setAttribute(SESSION_RESET_CODE, code);
                session.setAttribute(SESSION_RESET_EXPIRES_AT, expiresAt);
                session.removeAttribute(SESSION_RESET_VERIFIED);

                String subject = "密码重置验证码";
                String body = "您好，您正在进行密码重置操作。\n\n"
                        + "验证码：" + code + "\n"
                        + "有效期 5 分钟。\n\n"
                        + "若非本人操作，请忽略此邮件。";

                MailService.sendText(email, subject, body);

                req.setAttribute("message", "验证码已发送，请在 5 分钟内查收邮箱并填写验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;

            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }

        if ("verifyResetCode".equalsIgnoreCase(op)) {
            String username = trim(req.getParameter("username"));
            String code = trim(req.getParameter("code"));
            if (isEmpty(username) || isEmpty(code)) {
                req.setAttribute("error", "请填写用户名与验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            if (!verifyResetOtp(session, username, code, SESSION_RESET_USERNAME, SESSION_RESET_CODE, SESSION_RESET_EXPIRES_AT)) {
                req.setAttribute("error", "验证码错误或已过期，请重新获取验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            session.setAttribute(SESSION_RESET_VERIFIED, username);
            req.setAttribute("username", username);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
            return;
        }

        if ("doResetPassword".equalsIgnoreCase(op)) {
            String username = (String) session.getAttribute(SESSION_RESET_VERIFIED);
            if (isEmpty(username)) {
                req.setAttribute("error", "请先完成验证码校验。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            String password = trim(req.getParameter("password"));
            String confirm = trim(req.getParameter("confirmPassword"));
            if (isEmpty(password) || isEmpty(confirm)) {
                req.setAttribute("error", "请填写新密码并确认。");
                req.setAttribute("username", username);
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
                return;
            }
            if (!password.equals(confirm)) {
                req.setAttribute("error", "两次输入的密码不一致。");
                req.setAttribute("username", username);
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
                return;
            }
            if (password.length() < 6) {
                req.setAttribute("error", "密码长度至少 6 位。");
                req.setAttribute("username", username);
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
                return;
            }

            try {
                User user = userDAO.findByUsername(username);
                if (user == null) {
                    req.setAttribute("error", "用户不存在。");
                    req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                    return;
                }
                userDAO.resetPassword(user.getUserId(), password);

                
                session.removeAttribute(SESSION_RESET_VERIFIED);
                session.removeAttribute(SESSION_RESET_USERNAME);
                session.removeAttribute(SESSION_RESET_EMAIL);
                session.removeAttribute(SESSION_RESET_CODE);
                session.removeAttribute(SESSION_RESET_EXPIRES_AT);

                req.setAttribute("message", "密码已更新，请使用新密码登录。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                return;

            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }

        
        req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
    }

    private boolean verifyResetOtp(HttpSession session,
                                   String username,
                                   String code,
                                   String sessionUsernameKey,
                                   String sessionCodeKey,
                                   String sessionExpiresKey) {
        if (session == null) return false;
        Object su = session.getAttribute(sessionUsernameKey);
        Object sc = session.getAttribute(sessionCodeKey);
        Object sx = session.getAttribute(sessionExpiresKey);

        if (!(su instanceof String) || !(sc instanceof String) || sx == null) return false;
        String storedUsername = ((String) su).trim();
        String storedCode = ((String) sc).trim();

        long expiresAt;
        try {
            if (sx instanceof Long) {
                expiresAt = (Long) sx;
            } else {
                expiresAt = Long.parseLong(String.valueOf(sx));
            }
        } catch (Exception ignore) {
            return false;
        }

        if (System.currentTimeMillis() > expiresAt) return false;
        if (username == null || !storedUsername.equalsIgnoreCase(username.trim())) return false;
        return storedCode.equals(code == null ? "" : code.trim());
    }



    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String defaultString(String s) {
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

