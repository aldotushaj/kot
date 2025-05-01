package com.parkingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Root mapping to serve the index page
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Login page mapping
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}