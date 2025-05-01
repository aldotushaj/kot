package com.parkingsystem.controller;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.service.ParkingService;
import com.parkingsystem.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/parking")
public class ParkingController {

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

    // Show reservation form
    @GetMapping("/{id}/reserve")
    public String showReservationForm(@PathVariable Long id, Model model) {
        parkingService.getParkingById(id).ifPresent(parking -> {
            model.addAttribute("parking", parking);
        });
        return "user/reservation-form";
    }

    // Create reservation
    @PostMapping("/{id}/reserve")
    public String createReservation(
            @PathVariable Long id,
            @RequestParam String licensePlate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Model model) {

        try {
            Reservation reservation = reservationService.createReservation(licensePlate, startTime, endTime, id);
            model.addAttribute("reservation", reservation);
            return "user/reservation-success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "user/reservation-form";
        }
    }

    // Pay for reservation
    @PostMapping("/reservation/{id}/pay")
    public String payReservation(@PathVariable Long id, Model model) {
        try {
            Reservation reservation = reservationService.updatePaymentStatus(id, true);
            model.addAttribute("reservation", reservation);
            model.addAttribute("message", "Payment successful");
            return "user/payment-success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "user/payment-error";
        }
    }
}