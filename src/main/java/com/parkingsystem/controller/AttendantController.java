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

            List<Reservation> todaysReservations = reservationService.getReservationsByParkingAndTimeRange(
                    parkingId, startOfDay, endOfDay);

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

            // Filter to exclude today's reservations
            LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
            List<Reservation> pastReservations = allReservations.stream()
                    .filter(r -> r.getEndTime() != null)
                    .filter(r -> r.getEndTime().isBefore(startOfDay))
                    .sorted((r1, r2) -> r2.getEndTime().compareTo(r1.getEndTime())) // Compare in reverse order
                    .collect(Collectors.toList());
            model.addAttribute("parking", parking);
            model.addAttribute("pastReservations", pastReservations);
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