package edu.bjfu.journal.security;

import edu.bjfu.journal.model.SysOpLog;
import edu.bjfu.journal.model.SysUser;
import edu.bjfu.journal.service.LogService;
import edu.bjfu.journal.util.Constants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
public class OpLogAspect {

    @Autowired private LogService logService;
    @Autowired private HttpServletRequest request;

    @Around("@annotation(edu.bjfu.journal.security.OpLog)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Exception err = null;
        Object ret = null;

        try {
            ret = pjp.proceed();
            return ret;
        } catch (Exception e) {
            err = e;
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;

            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Method method = sig.getMethod();
            OpLog anno = method.getAnnotation(OpLog.class);

            SysUser u = (SysUser) request.getSession().getAttribute(Constants.SESSION_USER);

            SysOpLog log = new SysOpLog();
            if (u != null) {
                log.setUserId(u.getId());
                log.setUsername(u.getUsername());
            }
            log.setOpType("WEB");
            log.setModule(anno.module());
            log.setAction(anno.action());
            log.setMethod(request.getMethod());
            log.setPath(request.getRequestURI());
            log.setReqParams(request.getQueryString());
            log.setSuccess(err == null ? 1 : 0);
            log.setErrorMsg(err == null ? null : err.getMessage());
            log.setCostMs(cost);

            try {
                logService.addOpLog(log);
            } catch (Exception ignore) {}
        }
    }
}
