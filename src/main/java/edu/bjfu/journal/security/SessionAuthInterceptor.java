package edu.bjfu.journal.security;

import edu.bjfu.journal.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object user = request.getSession().getAttribute(Constants.SESSION_USER);
        String uri = request.getRequestURI();

        // 放行：登录页、登录接口、静态资源
        if (uri.endsWith("/login") || uri.contains("/auth/login") || uri.contains("/static/")) {
            return true;
        }
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }
}
