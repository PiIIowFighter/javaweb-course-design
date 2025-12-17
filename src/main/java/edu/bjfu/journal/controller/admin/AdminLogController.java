package edu.bjfu.journal.controller.admin;

import edu.bjfu.journal.security.RequiresPerm;
import edu.bjfu.journal.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/logs")
@RequiresPerm("SYS:LOG:VIEW")
public class AdminLogController {

    @Autowired private LogService logService;

    @GetMapping("/login")
    public String loginLogs(@RequestParam(required = false) String username, Model model) {
        model.addAttribute("logs", logService.loginLogs(username));
        model.addAttribute("username", username);
        return "admin/login_logs";
    }

    @GetMapping("/op")
    public String opLogs(@RequestParam(required = false) String username,
                         @RequestParam(required = false) String module,
                         Model model) {
        model.addAttribute("logs", logService.opLogs(username, module));
        model.addAttribute("username", username);
        model.addAttribute("module", module);
        return "admin/op_logs";
    }
}
