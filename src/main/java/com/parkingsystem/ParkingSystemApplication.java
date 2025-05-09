package com.parkingsystem;

import com.parkingsystem.model.Parking;
import com.parkingsystem.service.ParkingService;
import com.parkingsystem.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableScheduling  // Keep this annotation to enable scheduled tasks
public class ParkingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkingSystemApplication.class, args);
    }

    // Create admin users and fix any inconsistent data
    @Bean
    public CommandLineRunner loadData(UserService userService, ParkingService parkingService) {
        return args -> {
            // Create admin user if not exists
            try {
                if (userService.getUserByUsername("admin").isEmpty()) {
                    userService.createUser("admin", "admin123", "ADMIN");
                    System.out.println("Admin user created successfully");
                }

                // Create another admin user as requested
                if (userService.getUserByUsername("aldo").isEmpty()) {
                    userService.createUser("aldo", "pass123", "ADMIN");
                    System.out.println("Admin user 'aldo' created successfully");
                }
            } catch (Exception e) {
                System.err.println("Error creating admin user: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for more detailed debugging
            }

            // Fix any inconsistent parking data - KEEPING this important validation
            try {
                List<Parking> allParkings = parkingService.getAllParkings();
                for (Parking parking : allParkings) {
                    if (parking.getAvailableSpots() > parking.getTotalSpots()) {
                        System.out.println("Fixing inconsistent parking data for: " + parking.getLocation() +
                                " (was showing " + parking.getAvailableSpots() + "/" + parking.getTotalSpots() + " spots)");
                        parking.setAvailableSpots(parking.getTotalSpots());
                        parkingService.updateParking(parking);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fixing parking data: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}