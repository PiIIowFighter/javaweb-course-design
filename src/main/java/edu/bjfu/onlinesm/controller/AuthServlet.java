package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.mail.MailService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.sql.SQLException;

/**
 * 负责登录、注册、注销等基本认证流程。
 *
 * URL 约定（见 web.xml 中的 /auth/* 映射）：
 *  GET  /auth/login      显示登录页
 *  GET  /auth/register   显示注册页
 *  GET  /auth/reset      显示重置密码页（通过邮箱验证码找回密码）
 *  GET  /auth/logout     注销并返回首页
 *
 *  POST /auth/login      执行登录
 *  POST /auth/register   执行注册
 *  POST /auth/reset      执行密码重置（发送验证码 / 校验验证码 / 设置新密码）
 *
 * 本实现已经使用真正的 SQL Server 数据库（通过 UserDAO），
 * 不再依赖 SimpleUserStore 内存假数据。
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/auth/*"})
public class AuthServlet extends HttpServlet {

    // Session keys for registration / reset verification flows
    private static final String SESSION_KEY_REGISTER_EMAIL = "REGISTER_EMAIL";
    private static final String SESSION_KEY_REGISTER_CODE = "REGISTER_EMAIL_CODE";
    private static final String SESSION_KEY_REGISTER_EXPIRE_AT = "REGISTER_EMAIL_CODE_EXPIRE_AT";

    private static final String SESSION_KEY_RESET_EMAIL = "RESET_EMAIL";
    private static final String SESSION_KEY_RESET_CODE = "RESET_EMAIL_CODE";
    private static final String SESSION_KEY_RESET_EXPIRE_AT = "RESET_EMAIL_CODE_EXPIRE_AT";
    private static final String SESSION_KEY_RESET_USER_ID = "RESET_USER_ID";
    private static final String SESSION_KEY_RESET_VERIFIED = "RESET_VERIFIED";

    private final UserDAO userDAO = new UserDAO();

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

    /**
     * 登录：
     *  1. 校验用户名和密码非空；
     *  2. 从 SQL Server 按用户名查询用户；
     *  3. 校验密码是否匹配，以及账号状态（ACTIVE 才能登录）；
     *  4. 成功则将 User 放入 sessionScope.currentUser，并跳转 /dashboard。
     */
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

            // 登录成功
            req.getSession(true).setAttribute("currentUser", user);
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            throw new ServletException("登录时访问数据库出错", e);
        }
    }

    /**
     * 注册新用户（AUTHOR / REVIEWER）：
     *  1. 校验必填字段、密码长度以及两次密码是否一致；
     *  2. 检查用户名是否已存在；
     *  3. 新用户状态直接置为 ACTIVE（无需管理员激活）；
     *  4. 给出“注册成功，现在可以使用该账号登录”的提示（当前仍不自动登录）。
     */
    
    
    /**
     * 注册流程：
     *  - 支持“发送邮箱验证码”和“提交注册”两类操作（通过表单中的 op 参数区分）；
     *  - 先填写邮箱并点击“发送验证码”，系统生成 6 位数字验证码并发送到该邮箱；
     *  - 在 5 分钟有效期内，填写收到的验证码以及基本信息后才能成功创建账号。
     */
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	String[] opValues = req.getParameterValues("op");
        String op = null;
        if (opValues != null && opValues.length > 0) {
            op = trim(opValues[opValues.length - 1]);
        }
        if (op == null) {
            op = "doRegister";
        }

        String username = trim(req.getParameter("username"));
        String password = trim(req.getParameter("password"));
        String confirmPassword = trim(req.getParameter("confirmPassword"));
        String email = trim(req.getParameter("email"));
        String emailCode = trim(req.getParameter("emailCode"));
        String fullName = trim(req.getParameter("fullName"));
        String affiliation = trim(req.getParameter("affiliation"));
        String researchArea = trim(req.getParameter("researchArea"));
        String registerRole = trim(req.getParameter("registerRole")); // AUTHOR / REVIEWER

        HttpSession session = req.getSession();

        // 1）发送注册验证码
        if ("sendCode".equals(op)) {
            // 发送验证码前，同时检查“用户名是否已被占用”和“邮箱是否已注册”
            if (isEmpty(username)) {
                req.setAttribute("error", "请先填写用户名和邮箱，然后再点击“发送验证码”。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                return;
            }
            if (isEmpty(email)) {
                req.setAttribute("error", "请先填写邮箱，然后再点击“发送验证码”。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                return;
            }

            try {
                // 用户名唯一性检查
                User existingByUsername = userDAO.findByUsername(username);
                if (existingByUsername != null) {
                    req.setAttribute("error", "该用户名已被注册，请更换用户名。");
                    req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                    return;
                }

                // 同一邮箱只能注册一个账号：若邮箱已存在，则不给发送验证码
                User existingByEmail = userDAO.findByEmail(email);
                if (existingByEmail != null) {
                    req.setAttribute("error", "该邮箱已经注册过账号，请直接登录或使用“忘记密码”功能找回密码。");
                    req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                    return;
                }
            } catch (SQLException e) {
                throw new ServletException("检查用户名 / 邮箱是否已注册时访问数据库出错", e);
            }

  // 生成 6 位数字验证码，5 分钟内有效
            String code = generateVerificationCode();
            long expireAt = System.currentTimeMillis() + 5 * 60_000L;

            session.setAttribute(SESSION_KEY_REGISTER_EMAIL, email);
            session.setAttribute(SESSION_KEY_REGISTER_CODE, code);
            session.setAttribute(SESSION_KEY_REGISTER_EXPIRE_AT, expireAt);

            String subject = "【在线投稿系统】注册邮箱验证码";
            String textBody = "您正在注册在线投稿系统账号，验证码为：" + code + "，5 分钟内有效。如非本人操作，请忽略本邮件。";
            MailService.send(email, subject, textBody, null, null);

            req.setAttribute("message", "验证码已发送至邮箱，请在 5 分钟内输入 6 位数字验证码完成注册。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        // 2）提交注册表单，校验验证码并创建账户
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
            req.setAttribute("error", "邮箱不能为空，请填写注册邮箱并完成验证。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        // 校验邮箱验证码
        String sessionEmail = (String) session.getAttribute(SESSION_KEY_REGISTER_EMAIL);
        String sessionCode = (String) session.getAttribute(SESSION_KEY_REGISTER_CODE);
        Long expireAt = (Long) session.getAttribute(SESSION_KEY_REGISTER_EXPIRE_AT);

        if (sessionEmail == null || sessionCode == null || expireAt == null) {
            req.setAttribute("error", "请先获取邮箱验证码，再完成注册。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (!email.equalsIgnoreCase(sessionEmail)) {
            req.setAttribute("error", "当前填写的邮箱与获取验证码时不一致，请重新获取验证码。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (System.currentTimeMillis() > expireAt) {
            req.setAttribute("error", "验证码已过期，请重新获取。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (isEmpty(emailCode) || !sessionCode.equals(emailCode)) {
            req.setAttribute("error", "邮箱验证码不正确，请重新输入。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        // 缺省身份为 AUTHOR，允许用户选择注册为审稿人（REVIEWER）
        String targetRoleCode = "AUTHOR";
        if ("REVIEWER".equalsIgnoreCase(registerRole)) {
            targetRoleCode = "REVIEWER";
        }

        try {
            // 1) 用户名唯一检查
            if (userDAO.findByUsername(username) != null) {
                // 按任务书要求：提示“用户已存在，请重新注册”
                req.setAttribute("error", "用户已存在，请重新注册");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                return;
            }

            // 2) 邮箱唯一检查：同一邮箱只能注册一个账号
            User existingByEmail = userDAO.findByEmail(email);
            if (existingByEmail != null) {
                req.setAttribute("error", "该邮箱已经注册过账号，请直接登录或使用“忘记密码”功能找回密码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
                return;
            }

            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(password); // 简化：明文存储
            user.setEmail(defaultString(email));
            user.setFullName(defaultString(fullName));
            user.setAffiliation(defaultString(affiliation));
            user.setResearchArea(defaultString(researchArea));
            // 新注册用户默认状态为 ACTIVE，无需管理员审核即可登录
            user.setStatus("ACTIVE");

            if ("REVIEWER".equals(targetRoleCode)) {
                userDAO.createUserWithRole(user, "REVIEWER");
            } else {
                userDAO.registerAuthor(user);
            }

            // 注册完成后清理验证码相关 session
            session.removeAttribute(SESSION_KEY_REGISTER_EMAIL);
            session.removeAttribute(SESSION_KEY_REGISTER_CODE);
            session.removeAttribute(SESSION_KEY_REGISTER_EXPIRE_AT);

            // 不自动登录，直接提示可以使用该账号登录
            req.setAttribute("message", "注册成功，您的账户已激活，现在可以使用该账号登录系统。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("注册时访问数据库出错", e);
        }
    }


    /**
     * 忘记密码流程：
     *  1) sendResetCode：根据邮箱发送 6 位验证码；
     *  2) verifyResetCode：校验验证码正确后，进入设置新密码页面；
     *  3) doResetPassword：设置新密码并真正更新数据库。
     */
    private void handleReset(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 与注册流程类似，op 可能出现多次（隐藏域 + 按钮），取最后一个值作为本次操作
        String[] opValues = req.getParameterValues("op");
        String op = null;
        if (opValues != null && opValues.length > 0) {
            op = trim(opValues[opValues.length - 1]);
        }
        if (op == null) {
            op = "sendResetCode";
        }

        String email = trim(req.getParameter("email"));
        String resetCode = trim(req.getParameter("resetCode"));
        String newPassword = trim(req.getParameter("newPassword"));
        String confirmNewPassword = trim(req.getParameter("confirmNewPassword"));

        HttpSession session = req.getSession();

        // 1）发送重置验证码
        if ("sendResetCode".equals(op)) {
            if (isEmpty(email)) {
                req.setAttribute("error", "请先填写注册邮箱，再点击“发送验证码”。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            try {
                User user = userDAO.findByEmail(email);
                if (user == null) {
                    req.setAttribute("error", "该邮箱尚未注册账号，请确认后重新输入。");
                    req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                    return;
                }

                String code = generateVerificationCode();
                long expireAt = System.currentTimeMillis() + 5 * 60_000L;

                session.setAttribute(SESSION_KEY_RESET_EMAIL, email);
                session.setAttribute(SESSION_KEY_RESET_CODE, code);
                session.setAttribute(SESSION_KEY_RESET_EXPIRE_AT, expireAt);
                session.setAttribute(SESSION_KEY_RESET_USER_ID, user.getUserId());
                session.setAttribute(SESSION_KEY_RESET_VERIFIED, Boolean.FALSE);
                session.setAttribute("resetEmail", email); // JSP 展示用

                String subject = "【在线投稿系统】重置密码验证码";
                String textBody = "您正在重置在线投稿系统账号密码，验证码为：" + code + "，5 分钟内有效。如非本人操作，请忽略本邮件。";
                MailService.send(email, subject, textBody, null, null);

                req.setAttribute("message", "验证码已发送至邮箱，请在 5 分钟内输入 6 位数字验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
            } catch (SQLException e) {
                throw new ServletException("发送重置验证码时访问数据库出错", e);
            }
            return;
        }

        // 2）校验验证码，进入设置新密码页面
        if ("verifyResetCode".equals(op)) {
            String sessionEmail = (String) session.getAttribute(SESSION_KEY_RESET_EMAIL);
            String sessionCode = (String) session.getAttribute(SESSION_KEY_RESET_CODE);
            Long expireAt = (Long) session.getAttribute(SESSION_KEY_RESET_EXPIRE_AT);

            if (sessionEmail == null || sessionCode == null || expireAt == null) {
                req.setAttribute("error", "请先发送验证码，然后再输入验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }
            if (isEmpty(email) || !email.equalsIgnoreCase(sessionEmail)) {
                req.setAttribute("error", "当前填写的邮箱与接收验证码的邮箱不一致，请重新输入或重新发送验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }
            if (System.currentTimeMillis() > expireAt) {
                req.setAttribute("error", "验证码已过期，请重新发送。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }
            if (isEmpty(resetCode) || !sessionCode.equals(resetCode)) {
                req.setAttribute("error", "验证码不正确，请重新输入。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            session.setAttribute(SESSION_KEY_RESET_VERIFIED, Boolean.TRUE);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
            return;
        }

        // 3）真正重置密码
        if ("doResetPassword".equals(op)) {
            Boolean verified = (Boolean) session.getAttribute(SESSION_KEY_RESET_VERIFIED);
            Integer userId = (Integer) session.getAttribute(SESSION_KEY_RESET_USER_ID);
            String sessionEmail = (String) session.getAttribute(SESSION_KEY_RESET_EMAIL);

            if (verified == null || !verified || userId == null || sessionEmail == null) {
                req.setAttribute("error", "验证码校验未完成或会话已失效，请重新发送验证码。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
                return;
            }

            if (isEmpty(newPassword) || isEmpty(confirmNewPassword)) {
                req.setAttribute("error", "新密码和确认密码均不能为空。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
                return;
            }
            if (newPassword.length() < 8) {
                req.setAttribute("error", "新密码长度至少为 8 位，请重新输入。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
                return;
            }
            if (!newPassword.equals(confirmNewPassword)) {
                req.setAttribute("error", "两次输入的新密码不一致，请重新输入。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password_form.jsp").forward(req, resp);
                return;
            }

            try {
                userDAO.resetPassword(userId, newPassword);

                // 重置成功后，清理相关 session
                session.removeAttribute(SESSION_KEY_RESET_EMAIL);
                session.removeAttribute(SESSION_KEY_RESET_CODE);
                session.removeAttribute(SESSION_KEY_RESET_EXPIRE_AT);
                session.removeAttribute(SESSION_KEY_RESET_USER_ID);
                session.removeAttribute(SESSION_KEY_RESET_VERIFIED);
                session.removeAttribute("resetEmail");

                req.setAttribute("message", "密码重置成功，请使用新密码登录。");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                return;
            } catch (SQLException e) {
                throw new ServletException("重置密码时访问数据库出错", e);
            }
        }

        // 默认：打开重置页面
        req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
    }

// === 工具方法 ===

    /** 生成 6 位数字验证码。 */
    private static String generateVerificationCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(code);
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