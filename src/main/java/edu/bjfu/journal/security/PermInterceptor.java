package edu.bjfu.journal.security;

import edu.bjfu.journal.util.Constants;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

@Component
public class PermInterceptor implements HandlerInterceptor {

    @SuppressWarnings("unchecked")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;

        HandlerMethod hm = (HandlerMethod) handler;
        RequiresPerm anno = hm.getMethodAnnotation(RequiresPerm.class);
        if (anno == null) {
            anno = hm.getBeanType().getAnnotation(RequiresPerm.class);
        }
        if (anno == null) return true;

        Object permsObj = request.getSession().getAttribute(Constants.SESSION_PERMS);
        if (permsObj == null) {
            response.setStatus(403);
            response.getWriter().write("Forbidden: no perms in session");
            return false;
        }
        Set<String> perms = (Set<String>) permsObj;
        if (!perms.contains(anno.value())) {
            response.setStatus(403);
            response.getWriter().write("Forbidden: missing perm " + anno.value());
            return false;
        }
        return true;
    }
}
