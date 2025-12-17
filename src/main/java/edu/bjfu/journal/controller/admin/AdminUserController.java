package edu.bjfu.journal.controller.admin;

import edu.bjfu.journal.model.SysUser;
import edu.bjfu.journal.security.OpLog;
import edu.bjfu.journal.security.RequiresPerm;
import edu.bjfu.journal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiresPerm("SYS:USER:VIEW")
public class AdminUserController {

    @Autowired private UserService userService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       Model model) {
        List<SysUser> users = userService.list(keyword, status);
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/users";
    }

    @PostMapping("/create")
    @RequiresPerm("SYS:USER:EDIT")
    @OpLog(module = "SYS", action = "CreateUser")
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false) String realName,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String roleCode) {
        userService.createUser(username, password, realName, email, phone, 1, roleCode);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/approve")
    @RequiresPerm("SYS:USER:EDIT")
    @OpLog(module = "SYS", action = "ApproveUser")
    public String approve(@PathVariable Long id) {
        userService.approve(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/disable")
    @RequiresPerm("SYS:USER:EDIT")
    @OpLog(module = "SYS", action = "DisableUser")
    public String disable(@PathVariable Long id) {
        userService.disable(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    @RequiresPerm("SYS:USER:EDIT")
    @OpLog(module = "SYS", action = "DeleteUser")
    public String delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
