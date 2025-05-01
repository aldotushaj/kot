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
import java.util.List;
import java.util.Optional;

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

    // Show dashboard for the assigned parking lot
    @GetMapping("/dashboard/{parkingId}")
    public String showDashboard(@PathVariable Long parkingId, Model model) {
        // Check if parking exists
        parkingService.getParkingById(parkingId).ifPresent(parking -> {
            // Get active entries and reservations for this parking
            List<VehicleEntry> activeEntries = vehicleEntryService.getActiveEntriesByParking(parkingId);
            List<Reservation> currentReservations = reservationService.getReservationsByParking(parkingId);

            model.addAttribute("parking", parking);
            model.addAttribute("activeEntries", activeEntries);
            model.addAttribute("reservations", currentReservations);
        });

        return "attendant/dashboard";
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
            // Redirect back to dashboard
            return "redirect:/attendant/dashboard/" + vehicleEntryService.getReservationsByEntryId(entryId).getParking().getId();
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
