package com.oreilley.playthis1.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping ("/home")
    public String home(
        @RequestParam(value = "name", required = false,
                      defaultValue = "world")String name, Model model){
        model.addAttribute("user", name);
        return "Home";
    }
}
