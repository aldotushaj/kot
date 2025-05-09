package com.parkingsystem.controller;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.model.User;
import com.parkingsystem.model.VehicleEntry;
import com.parkingsystem.service.ParkingService;
import com.parkingsystem.service.ReservationService;
import com.parkingsystem.service.UserService;
import com.parkingsystem.service.VehicleEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendant")
public class AttendantController {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private VehicleEntryService vehicleEntryService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserService userService;

    // Default dashboard route - redirects to the assigned parking dashboard
    @GetMapping
    public String redirectToDashboard() {
        // Get current logged in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getAssignedParking() != null) {
            return "redirect:/attendant/dashboard/" + userOpt.get().getAssignedParking().getId();
        } else {
            // If no assigned parking, show an error page
            return "attendant/no-assignment";
        }
    }

    // Show dashboard for the assigned parking lot with today's reservations
    @GetMapping("/dashboard/{parkingId}")
    public String showDashboard(@PathVariable Long parkingId, Model model) {
        // Check if parking exists
        parkingService.getParkingById(parkingId).ifPresent(parking -> {
            // Get active entries and today's reservations for this parking
            List<VehicleEntry> activeEntries = vehicleEntryService.getActiveEntriesByParking(parkingId);

            // Get today's reservations
            LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
            LocalDateTime currentTime = LocalDateTime.now();

            List<Reservation> todaysReservations = reservationService.getReservationsByParkingAndTimeRange(
                    parkingId, startOfDay, endOfDay);

            // Sort reservations with complex logic:
            // 1. Active/upcoming reservations first (end time > current time or null end time)
            // 2. Completed reservations after (end time <= current time)
            // 3. Within each group, sort by start time (newest first)
            todaysReservations.sort((r1, r2) -> {
                boolean r1Active = r1.getEndTime() == null || r1.getEndTime().isAfter(currentTime);
                boolean r2Active = r2.getEndTime() == null || r2.getEndTime().isAfter(currentTime);

                // If one is active and the other is not, the active one comes first
                if (r1Active && !r2Active) return -1;
                if (!r1Active && r2Active) return 1;

                // If both are in the same category (both active or both completed),
                // sort by start time (newest first)
                return r2.getStartTime().compareTo(r1.getStartTime());
            });

            model.addAttribute("parking", parking);
            model.addAttribute("activeEntries", activeEntries);
            model.addAttribute("reservations", todaysReservations);
        });

        return "attendant/dashboard";
    }

    // Show reservation history for the parking lot
    @GetMapping("/history/{parkingId}")
    public String showReservationHistory(@PathVariable Long parkingId, Model model) {
        // Check if parking exists
        parkingService.getParkingById(parkingId).ifPresent(parking -> {
            // Get all reservations for this parking
            List<Reservation> allReservations = reservationService.getReservationsByParking(parkingId);

            // Current date for filtering
            LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);

            // Filter and separate the reservations into two categories:
            // 1. App reservations - reservedFromApp is true
            // 2. Walk-in reservations - reservedFromApp is false

            // Filter app reservations (past only)
            List<Reservation> appReservations = allReservations.stream()
                    .filter(r -> r.isReservedFromApp()) // App reservations
                    .filter(r -> r.getEndTime() != null)
                    .filter(r -> r.getEndTime().isBefore(startOfDay))
                    .sorted((r1, r2) -> r2.getEndTime().compareTo(r1.getEndTime())) // Compare in reverse order
                    .collect(Collectors.toList());

            // Filter walk-in reservations (all past, including TODAY's completed walk-ins)
            List<Reservation> walkInReservations = allReservations.stream()
                    .filter(r -> !r.isReservedFromApp()) // Walk-in reservations
                    .filter(r -> r.getEndTime() != null) // Only completed ones
                    .sorted((r1, r2) -> r2.getEndTime().compareTo(r1.getEndTime())) // Compare in reverse order
                    .collect(Collectors.toList());

            model.addAttribute("parking", parking);
            model.addAttribute("appReservations", appReservations);
            model.addAttribute("walkInReservations", walkInReservations);
        });

        return "attendant/reservation-history";
    }
    // Register vehicle check-in
    @PostMapping("/{parkingId}/checkin")
    public String registerVehicleEntry(
            @PathVariable Long parkingId,
            @RequestParam String licensePlate,
            @RequestParam(defaultValue = "false") boolean reservedFromApp,
            Model model) {

        try {
            VehicleEntry entry = vehicleEntryService.registerEntry(licensePlate, parkingId, reservedFromApp);
            model.addAttribute("entry", entry);
            model.addAttribute("message", "Vehicle checked in successfully");
            return "redirect:/attendant/dashboard/" + parkingId;
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/attendant/dashboard/" + parkingId;
        }
    }

    // Register vehicle check-out
    @PostMapping("/checkout/{entryId}")
    public String registerVehicleExit(
            @PathVariable Long entryId,
            Model model) {

        try {
            VehicleEntry entry = vehicleEntryService.registerExit(entryId);
            model.addAttribute("entry", entry);
            model.addAttribute("message", "Vehicle checked out successfully");

            // If this was a walk-in vehicle (not from app) and we have created a reservation record
            if (!entry.isReservedFromApp() && entry.getCheckoutReservationId() != null) {
                // Redirect to receipt page with the reservation ID
                return "redirect:/attendant/receipt/" + entry.getCheckoutReservationId();
            }

            return "redirect:/attendant/dashboard/" + entry.getParking().getId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());

            // Fixed: Get the parking ID correctly from the entry
            try {
                VehicleEntry entry = vehicleEntryService.getVehicleEntryByEntryId(entryId);
                return "redirect:/attendant/dashboard/" + entry.getParking().getId();
            } catch (Exception ex) {
                // If we can't get the entry, redirect to the general attendant page
                return "redirect:/attendant";
            }
        }
    }
    @GetMapping("/receipt/{reservationId}")
    public String showReceipt(
            @PathVariable Long reservationId,
            Model model) {

        try {
            Optional<Reservation> reservationOpt = reservationService.getReservationById(reservationId);

            if (reservationOpt.isPresent()) {
                Reservation reservation = reservationOpt.get();
                model.addAttribute("reservation", reservation);

                // Calculate the duration in hours and minutes
                long minutes = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
                long hours = minutes / 60;
                long remainingMinutes = minutes % 60;

                model.addAttribute("durationHours", hours);
                model.addAttribute("durationMinutes", remainingMinutes);

                return "attendant/receipt";
            } else {
                model.addAttribute("error", "Receipt not found");
                return "redirect:/attendant";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/attendant";
        }
    }
    // Search for reservations by license plate
    @GetMapping("/{parkingId}/search")
    public String searchReservations(
            @PathVariable Long parkingId,
            @RequestParam String licensePlate,
            Model model) {

        List<Reservation> reservations = reservationService.findReservationsByLicensePlate(licensePlate);
        model.addAttribute("reservations", reservations);
        model.addAttribute("parkingId", parkingId);

        return "attendant/search-results";
    }
}