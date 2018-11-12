package com.oreilley.playthis1.clientControllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String homePage(@RequestParam(value = "name",
            defaultValue = "world", required = false) String name, Model model) {
        model.addAttribute("user", name);
        return "home";
    }
}
