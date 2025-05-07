package com.parkingsystem.controller;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.service.ParkingService;
import com.parkingsystem.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/parking")
public class ParkingController {

    private static final Logger logger = LoggerFactory.getLogger(ParkingController.class);

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private ReservationService reservationService;

    // Home page redirect
    @GetMapping("/")
    public String home() {
        return "redirect:/parking";
    }

    // Show all parking locations
    @GetMapping
    public String showAllParkings(Model model) {
        List<Parking> parkings = parkingService.findAvailableParking();
        model.addAttribute("parkings", parkings);
        return "user/parking-list";
    }

    // Show details for a specific parking location
    @GetMapping("/{id}")
    public String showParkingDetails(@PathVariable Long id, Model model) {
        parkingService.getParkingById(id).ifPresent(parking -> {
            model.addAttribute("parking", parking);
        });
        return "user/parking-details";
    }

    // Show reservation form - requires authentication
    @GetMapping("/{id}/reserve")
    public String showReservationForm(@PathVariable Long id, Model model) {
        // Log the request to help with debugging
        logger.info("Showing reservation form for parking ID: {}", id);

        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated user attempting to access reservation form");
            return "redirect:/login";
        }

        try {
            parkingService.getParkingById(id).ifPresent(parking -> {
                model.addAttribute("parking", parking);

                // Add default times - current time + 1 hour for end time
                LocalDateTime now = LocalDateTime.now();
                // Round to next hour
                LocalDateTime startTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
                LocalDateTime endTime = startTime.plusHours(1);

                model.addAttribute("startTime", startTime);
                model.addAttribute("endTime", endTime);
                logger.info("Added default times: start={}, end={}", startTime, endTime);
            });
            return "user/reservation-form";
        } catch (Exception e) {
            logger.error("Error showing reservation form: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while loading the reservation form: " + e.getMessage());
            return "error";
        }
    }

    // Create reservation - requires authentication
    @PostMapping("/{id}/reserve")
    public String createReservation(
            @PathVariable Long id,
            @RequestParam String licensePlate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Model model) {

        logger.info("Creating reservation: parkingId={}, licensePlate={}, startTime={}, endTime={}",
                id, licensePlate, startTime, endTime);

        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated user attempting to create reservation");
            return "redirect:/login";
        }

        // Get current username
        String username = authentication.getName();

        try {
            // Pass username to reservation service
            Reservation reservation = reservationService.createReservation(
                    licensePlate, startTime, endTime, id, username);
            logger.info("Reservation created successfully with ID: {}", reservation.getId());

            // Add reservation to model for the success page
            model.addAttribute("reservation", reservation);
            return "user/reservation-success";

        } catch (Exception e) {
            logger.error("Error creating reservation: {}", e.getMessage(), e);

            // Add error message to model for the form page
            model.addAttribute("error", e.getMessage());

            // Re-add the parking information
            parkingService.getParkingById(id).ifPresent(parking -> {
                model.addAttribute("parking", parking);
                model.addAttribute("licensePlate", licensePlate);
                model.addAttribute("startTime", startTime);
                model.addAttribute("endTime", endTime);
            });

            return "user/reservation-form";
        }
    }

    // Pay for reservation - requires authentication
    @PostMapping("/reservation/{id}/pay")
    public String payReservation(
            @PathVariable Long id,
            Model model) {

        logger.info("Processing payment for reservation ID: {}", id);

        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated user attempting to pay for reservation");
            return "redirect:/login";
        }

        try {
            Reservation reservation = reservationService.updatePaymentStatus(id, true);
            logger.info("Payment successful for reservation ID: {}", id);

            model.addAttribute("reservation", reservation);
            model.addAttribute("message", "Payment successful");
            return "user/payment-success";

        } catch (Exception e) {
            logger.error("Payment error for reservation ID {}: {}", id, e.getMessage(), e);

            model.addAttribute("error", e.getMessage());
            return "user/payment-error";
        }
    }
}