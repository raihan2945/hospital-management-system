package com.example.hms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class HomeController {

    @ModelAttribute("activeSection")
    public String activeSection() { return "home"; }

    @GetMapping({"/", "/dashboard"})
    public String home() {
        return "home";
    }
}
