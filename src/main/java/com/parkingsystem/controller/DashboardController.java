package com.parkingsystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String redirectToDashboard(Authentication authentication) {
        // Redirect to appropriate dashboard based on role
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ATTENDANT"))) {
            return "redirect:/attendant";
        } else {
            // Default to user dashboard
            return "redirect:/user/dashboard";
        }
    }
}