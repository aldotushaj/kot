package com.parkingsystem.controller;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.User;
import com.parkingsystem.service.ParkingService;
import com.parkingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private UserService userService;

    // Dashboard
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<Parking> parkings = parkingService.getAllParkings();
        model.addAttribute("parkings", parkings);
        return "admin/dashboard";
    }

    // Create new parking form
    @GetMapping("/parking/create")
    public String showCreateParkingForm() {
        return "admin/create-parking";
    }

    // Create new parking
    @PostMapping("/parking/create")
    public String createParking(
            @RequestParam String location,
            @RequestParam int totalSpots,
            @RequestParam BigDecimal hourlyRate,
            @RequestParam(required = false) String mapLink,
            Model model) {

        try {
            Parking parking = parkingService.createParking(location, totalSpots, hourlyRate, mapLink);
            model.addAttribute("message", "Parking location created successfully");
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/create-parking";
        }
    }

    // Edit parking form
    @GetMapping("/parking/{id}/edit")
    public String showEditParkingForm(@PathVariable Long id, Model model) {
        parkingService.getParkingById(id).ifPresent(parking -> {
            model.addAttribute("parking", parking);
        });
        return "admin/edit-parking";
    }

    // Update parking
    @PostMapping("/parking/{id}/edit")
    public String updateParking(
            @PathVariable Long id,
            @RequestParam String location,
            @RequestParam int totalSpots,
            @RequestParam BigDecimal hourlyRate,
            @RequestParam(required = false) String mapLink,
            Model model) {

        Optional<Parking> parkingOpt = parkingService.getParkingById(id);

        if (parkingOpt.isPresent()) {
            Parking parking = parkingOpt.get();
            parking.setLocation(location);
            parking.setTotalSpots(totalSpots);
            parking.setHourlyRate(hourlyRate);
            parking.setMapLink(mapLink);

            parkingService.updateParking(parking);
            model.addAttribute("message", "Parking updated successfully");
            return "redirect:/admin/dashboard";
        } else {
            model.addAttribute("error", "Parking not found");
            return "admin/dashboard";
        }
    }

    // Delete parking
    @PostMapping("/parking/{id}/delete")
    public String deleteParking(@PathVariable Long id, Model model) {
        try {
            parkingService.deleteParking(id);
            model.addAttribute("message", "Parking deleted successfully");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // Manage attendants
    @GetMapping("/attendants")
    public String manageAttendants(Model model) {
        List<User> attendants = userService.getAllAttendants();
        List<Parking> parkings = parkingService.getAllParkings();

        model.addAttribute("attendants", attendants);
        model.addAttribute("parkings", parkings);

        return "admin/manage-attendants";
    }

    // Assign attendant to parking
    @PostMapping("/attendant/assign")
    public String assignAttendantToParking(
            @RequestParam Long userId,
            @RequestParam Long parkingId,
            Model model) {

        try {
            userService.assignAttendantToParking(userId, parkingId);
            model.addAttribute("message", "Attendant assigned to parking successfully");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "redirect:/admin/attendants";
    }

    // Create new attendant form
    @GetMapping("/attendant/create")
    public String showCreateAttendantForm() {
        return "admin/create-attendant";
    }

    // Create new attendant
    @PostMapping("/attendant/create")
    public String createAttendant(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        try {
            userService.createUser(username, password, "ATTENDANT");
            model.addAttribute("message", "Attendant created successfully");
            return "redirect:/admin/attendants";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/create-attendant";
        }
    }

    // Remove attendant from parking
    @PostMapping("/attendant/{id}/remove-assignment")
    public String removeAttendantFromParking(
            @PathVariable Long id,
            Model model) {

        try {
            userService.removeAttendantFromParking(id);
            model.addAttribute("message", "Attendant removed from parking successfully");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "redirect:/admin/attendants";
    }
}