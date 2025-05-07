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

            // Get only the current user's reservations
            List<Reservation> userReservations = reservationRepository.findByUsername(username);

            if (userReservations == null) {
                userReservations = new ArrayList<>();
            }

            logger.info("Found {} reservations for user {}", userReservations.size(), username);

            // Filter for active and future reservations only (not expired)
            List<Reservation> activeAndFutureReservations = userReservations.stream()
                    .filter(r -> r.getEndTime() != null && r.getEndTime().isAfter(now))
                    .collect(Collectors.toList());

            // Create paid and unpaid lists for the dashboard - only include active/future reservations
            List<Reservation> paidReservations = activeAndFutureReservations.stream()
                    .filter(Reservation::isPaid)
                    .collect(Collectors.toList());

            List<Reservation> unpaidReservations = activeAndFutureReservations.stream()
                    .filter(r -> !r.isPaid())
                    .collect(Collectors.toList());

            // Add lists to the model
            model.addAttribute("paidReservations", paidReservations);
            model.addAttribute("unpaidReservations", unpaidReservations);

            logger.info("Added {} paid and {} unpaid active/future reservations to model",
                    paidReservations.size(), unpaidReservations.size());
        } catch (Exception e) {
            logger.error("Error retrieving reservations: {}", e.getMessage(), e);
            model.addAttribute("paidReservations", new ArrayList<>());
            model.addAttribute("unpaidReservations", new ArrayList<>());
            model.addAttribute("error", "Could not retrieve reservations: " + e.getMessage());
        }

        return "user/dashboard";
    }

    // Reservation history method remains the same
    @GetMapping("/history")
    public String reservationHistory(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);

        try {
            LocalDateTime now = LocalDateTime.now();

            // Get only the current user's reservations
            List<Reservation> userReservations = reservationRepository.findByUsername(username);

            if (userReservations == null) {
                userReservations = new ArrayList<>();
            }

            // Filter for past reservations (end time is in the past)
            List<Reservation> pastReservations = userReservations.stream()
                    .filter(r -> r.getEndTime() != null)
                    .filter(r -> r.getEndTime().isBefore(now))
                    .sorted((r1, r2) -> r2.getEndTime().compareTo(r1.getEndTime())) // Newest first
                    .collect(Collectors.toList());

            model.addAttribute("pastReservations", pastReservations);
            logger.info("Added {} past reservations to history model for user {}",
                    pastReservations.size(), username);

        } catch (Exception e) {
            logger.error("Error retrieving reservation history: {}", e.getMessage(), e);
            model.addAttribute("pastReservations", new ArrayList<>());
            model.addAttribute("error", "Could not retrieve reservation history: " + e.getMessage());
        }

        return "user/reservation-history";
    }
}