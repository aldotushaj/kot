package com.parkingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Root mapping redirects to login page
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    // Login page mapping
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}