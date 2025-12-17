package edu.bjfu.journal.controller.stub;

import edu.bjfu.journal.util.Constants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ✅ 其他组员模块的“先跑通骨架”入口：
 * - 你们集成时可以把这些页面替换为各自真正的页面与Controller。
 */
@Controller
@RequestMapping("/stub")
public class ModuleStubController {

    @GetMapping("/eic")
    public String eic() { return "stub/eic"; }

    @GetMapping("/editor")
    public String editor() { return "stub/editor"; }

    @GetMapping("/reviewer")
    public String reviewer() { return "stub/reviewer"; }

    @GetMapping("/author")
    public String author() { return "stub/author"; }

    @GetMapping("/cms")
    public String cms() { return "stub/cms"; }
}
