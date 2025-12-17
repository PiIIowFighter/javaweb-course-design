package edu.bjfu.journal.controller;

import edu.bjfu.journal.model.SysUser;
import edu.bjfu.journal.util.Constants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping({"/", "/app/home"})
    public String home(HttpServletRequest request, Model model) {
        SysUser user = (SysUser) request.getSession().getAttribute(Constants.SESSION_USER);
        model.addAttribute("user", user);

        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) request.getSession().getAttribute(Constants.SESSION_ROLES);
        model.addAttribute("roles", roles);

        return "home";
    }
}
