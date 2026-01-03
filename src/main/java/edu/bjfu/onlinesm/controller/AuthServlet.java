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
import java.sql.SQLException;
import java.security.SecureRandom;

/**
 * 负责登录、注册、注销等基本认证流程。
 *
 * URL 约定（见 web.xml 中的 /auth/* 映射）：
 *  GET  /auth/login      显示登录页
 *  GET  /auth/register   显示注册页
 *  GET  /auth/reset      显示重置密码页（占位）
 *  GET  /auth/logout     注销并返回首页
 *
 *  POST /auth/login      执行登录
 *  POST /auth/register   执行注册
 *  POST /auth/reset      执行密码重置（当前仅给出提示信息）
 *
 * 本实现已经使用真正的 SQL Server 数据库（通过 UserDAO），
 * 不再依赖 SimpleUserStore 内存假数据。
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/auth/*"})
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    // === Register email OTP (simple session-based) ===
    private static final String OTP_SESSION_CODE = "register_email_code";
    private static final String OTP_SESSION_EMAIL = "register_email_code_email";
    private static final String OTP_SESSION_EXPIRE_AT = "register_email_code_expire_at";
    private static final long OTP_TTL_MILLIS = 5L * 60L * 1000L; // 5 minutes
    private static final SecureRandom OTP_RNG = new SecureRandom();

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
     *  3. 新用户状态置为 PENDING，调用 UserDAO 写入数据库；
     *  4. 给出“注册成功，待管理员审核激活后方可登录”的提示，不自动登录。
     */
    
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // If user clicks "发送验证码" (AJAX or fallback), do NOT submit registration.
        String op = trim(req.getParameter("op"));
        if (op != null && "sendCode".equalsIgnoreCase(op)) {
            handleSendRegisterEmailCode(req, resp);
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
        String registerRole = trim(req.getParameter("registerRole")); // AUTHOR / REVIEWER

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

        // Email & verification code required
        if (isEmpty(email)) {
            req.setAttribute("error", "邮箱不能为空，请先填写邮箱并发送验证码。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (isEmpty(emailCode)) {
            req.setAttribute("error", "请输入邮箱验证码。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }
        if (!validateRegisterEmailCode(req.getSession(false), email, emailCode)) {
            req.setAttribute("error", "邮箱验证码错误或已过期，请重新发送验证码。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        // 缺省身份为 AUTHOR，允许用户选择注册为审稿人（REVIEWER）
        String targetRoleCode = "AUTHOR";
        if ("REVIEWER".equalsIgnoreCase(registerRole)) {
            targetRoleCode = "REVIEWER";
        }

        try {
            if (userDAO.findByUsername(username) != null) {
                // 按任务书要求：提示“用户已存在，请重新注册”
                req.setAttribute("error", "用户已存在，请重新注册");
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
            // 按需求：新注册用户直接激活
            user.setStatus("ACTIVE");

            if ("REVIEWER".equals(targetRoleCode)) {
                userDAO.createUserWithRole(user, "REVIEWER");
            } else {
                userDAO.registerAuthor(user);
            }

            // 清理验证码，避免重复使用
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.removeAttribute(OTP_SESSION_CODE);
                s.removeAttribute(OTP_SESSION_EMAIL);
                s.removeAttribute(OTP_SESSION_EXPIRE_AT);
            }

            // 不自动登录，提示可直接登录
            req.setAttribute("message", "注册成功，账号已激活，可直接登录。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("注册时访问数据库出错", e);
        }
    }

    /**
     * 发送注册邮箱验证码（6位数字）。
     * - 用 session 存储验证码与过期时间；
     * - 支持 AJAX：直接返回 text/plain，不刷新页面。
     */
    private void handleSendRegisterEmailCode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = trim(req.getParameter("email"));
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain;charset=UTF-8");

        if (isEmpty(email)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("请先填写邮箱");
            return;
        }

        String code = generate6DigitCode();
        long expireAt = System.currentTimeMillis() + OTP_TTL_MILLIS;
        HttpSession session = req.getSession(true);
        session.setAttribute(OTP_SESSION_CODE, code);
        session.setAttribute(OTP_SESSION_EMAIL, email);
        session.setAttribute(OTP_SESSION_EXPIRE_AT, expireAt);

        String subject = "注册验证码";
        String body = "您的注册验证码为：" + code + "\n\n" +
                "验证码 5 分钟内有效，请勿泄露给他人。";
        MailService.sendText(email, subject, body);

        resp.getWriter().write("验证码已发送，请查收邮箱（5分钟有效）");
    }

    private static boolean validateRegisterEmailCode(HttpSession session, String email, String codeInput) {
        if (session == null) return false;
        Object codeObj = session.getAttribute(OTP_SESSION_CODE);
        Object emailObj = session.getAttribute(OTP_SESSION_EMAIL);
        Object expObj = session.getAttribute(OTP_SESSION_EXPIRE_AT);
        if (!(codeObj instanceof String) || !(emailObj instanceof String) || expObj == null) return false;
        String code = (String) codeObj;
        String codeEmail = (String) emailObj;
        long expireAt;
        if (expObj instanceof Long) {
            expireAt = (Long) expObj;
        } else if (expObj instanceof String) {
            try {
                expireAt = Long.parseLong((String) expObj);
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }

        if (expireAt <= System.currentTimeMillis()) return false;
        if (email == null || !email.equalsIgnoreCase(codeEmail)) return false;
        return codeInput != null && codeInput.trim().equals(code);
    }

    private static String generate6DigitCode() {
        int v = OTP_RNG.nextInt(900000) + 100000; // 100000 - 999999
        return String.valueOf(v);
    }

    /**
     * 密码重置占位实现：当前仅给出提示信息，不做真实重置。
     */
    private void handleReset(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("message", "密码重置功能暂未实现，请联系管理员或稍后自行补充实现。");
        req.getRequestDispatcher("/WEB-INF/jsp/auth/reset_password.jsp").forward(req, resp);
    }

    // === 工具方法 ===

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