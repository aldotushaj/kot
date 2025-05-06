package com.parkingsystem.controller;

import com.parkingsystem.model.Reservation;
import com.parkingsystem.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> allReservations = reservationRepository.findAll();

            // Filter for active reservations (current time is between start and end times)
            // Adding null checks to prevent NullPointerException
            List<Reservation> activeReservations = allReservations.stream()
                    .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                    .filter(r -> r.getStartTime().isBefore(now) && r.getEndTime().isAfter(now))
                    .collect(Collectors.toList());

            // Filter for upcoming reservations (start time is in the future)
            // Adding null check to prevent NullPointerException
            List<Reservation> upcomingReservations = allReservations.stream()
                    .filter(r -> r.getStartTime() != null)
                    .filter(r -> r.getStartTime().isAfter(now))
                    .sorted(Comparator.comparing(Reservation::getStartTime))
                    .collect(Collectors.toList());

            // Add lists to the model
            model.addAttribute("activeReservations", activeReservations);
            model.addAttribute("upcomingReservations", upcomingReservations);

            logger.info("Added {} active and {} upcoming reservations to model",
                    activeReservations.size(), upcomingReservations.size());
        } catch (Exception e) {
            logger.error("Error retrieving reservations: {}", e.getMessage(), e);
            model.addAttribute("activeReservations", new ArrayList<>());
            model.addAttribute("upcomingReservations", new ArrayList<>());
            model.addAttribute("error", "Could not retrieve reservations: " + e.getMessage());
        }

        return "user/dashboard";
    }

    // Add new endpoint for reservation history
    @GetMapping("/history")
    public String reservationHistory(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> allReservations = reservationRepository.findAll();

            // Filter for past reservations (end time is in the past)
            // Adding null check to prevent NullPointerException
            List<Reservation> pastReservations = allReservations.stream()
                    .filter(r -> r.getEndTime() != null)
                    .filter(r -> r.getEndTime().isBefore(now))
                    .sorted(Comparator.comparing(Reservation::getEndTime).reversed())
                    .collect(Collectors.toList());

            model.addAttribute("pastReservations", pastReservations);
            logger.info("Added {} past reservations to history model", pastReservations.size());

        } catch (Exception e) {
            logger.error("Error retrieving reservation history: {}", e.getMessage(), e);
            model.addAttribute("pastReservations", new ArrayList<>());
            model.addAttribute("error", "Could not retrieve reservation history: " + e.getMessage());
        }

        return "user/reservation-history";
    }
}