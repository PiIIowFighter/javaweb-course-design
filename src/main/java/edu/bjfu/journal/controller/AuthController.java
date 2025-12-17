package edu.bjfu.journal.controller;

import edu.bjfu.journal.model.SysUser;
import edu.bjfu.journal.service.AuthService;
import edu.bjfu.journal.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/auth/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request) {
        SysUser u = authService.login(username, password, request);
        if (u == null) {
            request.setAttribute("error", "登录失败：账号/密码/状态不正确");
            return "login";
        }

        request.getSession().setAttribute(Constants.SESSION_USER, u);

        Set<String> roleCodes = authService.roleCodesOf(u.getId());
        request.getSession().setAttribute(Constants.SESSION_ROLES, roleCodes);

        List<Long> roleIds = authService.rolesOf(u.getId()).stream().map(r -> r.getId()).collect(Collectors.toList());
        Set<String> permCodes = authService.permsOfRoleIds(roleIds);
        request.getSession().setAttribute(Constants.SESSION_PERMS, permCodes);

        return "redirect:/app/home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/login";
    }
}
