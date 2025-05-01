package com.parkingsystem.controller;

import com.parkingsystem.model.Reservation;
import com.parkingsystem.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // Use ReservationRepository directly instead of the service
    @Autowired
    private ReservationRepository reservationRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);

        try {
            // Get all reservations from the repository
            List<Reservation> allReservations = reservationRepository.findAll();
            logger.info("Found {} total reservations", allReservations.size());

            // Filter for paid reservations
            List<Reservation> paidReservations = allReservations.stream()
                    .filter(Reservation::isPaid)
                    .collect(Collectors.toList());

            // Filter for unpaid reservations
            List<Reservation> unpaidReservations = allReservations.stream()
                    .filter(r -> !r.isPaid())
                    .collect(Collectors.toList());

            // Add both lists to the model
            model.addAttribute("paidReservations", paidReservations);
            model.addAttribute("unpaidReservations", unpaidReservations);

            logger.info("Added {} paid and {} unpaid reservations to model",
                    paidReservations.size(), unpaidReservations.size());
        } catch (Exception e) {
            logger.error("Error retrieving reservations: {}", e.getMessage(), e);
            // Add empty lists to model as fallback
            model.addAttribute("paidReservations", new ArrayList<>());
            model.addAttribute("unpaidReservations", new ArrayList<>());
            model.addAttribute("error", "Could not retrieve reservations: " + e.getMessage());
        }

        return "user/dashboard";
    }
}