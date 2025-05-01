package com.parkingsystem;

import com.parkingsystem.model.Parking;
import com.parkingsystem.service.ParkingService;
import com.parkingsystem.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class ParkingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkingSystemApplication.class, args);
    }

    // Inject services through method parameters to avoid circular dependencies
    @Bean
    public CommandLineRunner loadData(UserService userService, ParkingService parkingService) {
        return args -> {
            // Create admin user if not exists
            try {
                if (userService.getUserByUsername("admin").isEmpty()) {
                    userService.createUser("admin", "admin123", "ADMIN");
                    System.out.println("Admin user created successfully");
                }
            } catch (Exception e) {
                System.err.println("Error creating admin user: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for more detailed debugging
            }

            // Create regular user if not exists
            try {
                if (userService.getUserByUsername("user").isEmpty()) {
                    userService.createUser("user", "password", "USER");
                    System.out.println("Regular user created successfully");
                }
            } catch (Exception e) {
                System.err.println("Error creating regular user: " + e.getMessage());
                e.printStackTrace();
            }

            // Create some sample parking lots if database is empty
            try {
                if (parkingService.getAllParkings().isEmpty()) {
                    Parking downtown = parkingService.createParking("Downtown Parking", 50, new BigDecimal("5.00"));
                    Parking airport = parkingService.createParking("Airport Parking", 100, new BigDecimal("10.00"));
                    Parking mall = parkingService.createParking("Shopping Mall Parking", 200, new BigDecimal("3.50"));

                    System.out.println("Sample parking locations created");

                    // Create attendant users for each parking
                    userService.createUser("attendant1", "pass123", "ATTENDANT");
                    userService.assignAttendantToParking(2L, downtown.getId());

                    userService.createUser("attendant2", "pass123", "ATTENDANT");
                    userService.assignAttendantToParking(3L, airport.getId());

                    userService.createUser("attendant3", "pass123", "ATTENDANT");
                    userService.assignAttendantToParking(4L, mall.getId());

                    System.out.println("Sample attendants created and assigned");
                }
            } catch (Exception e) {
                System.err.println("Error creating sample data: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for more detailed debugging
            }
        };
    }
}