package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.model.User;
import com.parkingsystem.model.VehicleEntry;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.ReservationRepository;
import com.parkingsystem.repository.UserRepository;
import com.parkingsystem.repository.VehicleEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ParkingService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingService.class);

    private final ParkingRepository parkingRepository;
    private final UserRepository userRepository;
    private final VehicleEntryRepository vehicleEntryRepository;
    private final ReservationRepository reservationRepository;

    @Autowired
    public ParkingService(ParkingRepository parkingRepository,
                          UserRepository userRepository,
                          VehicleEntryRepository vehicleEntryRepository,
                          ReservationRepository reservationRepository) {
        this.parkingRepository = parkingRepository;
        this.userRepository = userRepository;
        this.vehicleEntryRepository = vehicleEntryRepository;
        this.reservationRepository = reservationRepository;
    }

    // Get all parking locations
    public List<Parking> getAllParkings() {
        return parkingRepository.findAll();
    }

    // Get parking by id
    public Optional<Parking> getParkingById(Long id) {
        return parkingRepository.findById(id);
    }

    // Find available parking spots
    public List<Parking> findAvailableParking() {
        return parkingRepository.findByAvailableSpotsGreaterThan(0);
    }

    // Create new parking location
    @Transactional
    public Parking createParking(String location, int totalSpots, BigDecimal hourlyRate, String mapLink) {
        if (location == null || location.trim().isEmpty()) {
            throw new RuntimeException("Location name is required");
        }

        if (totalSpots <= 0) {
            throw new RuntimeException("Total spots must be greater than zero");
        }

        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Hourly rate must be greater than zero");
        }

        Parking parking = new Parking(location, totalSpots, hourlyRate);
        parking.setMapLink(mapLink); // Set the map link
        return parkingRepository.save(parking);
    }

    // Update parking info
    @Transactional
    public Parking updateParking(Parking parking) {
        if (parking == null) {
            throw new RuntimeException("Parking cannot be null");
        }

        if (!parkingRepository.existsById(parking.getId())) {
            throw new RuntimeException("Parking not found with id: " + parking.getId());
        }

        return parkingRepository.save(parking);
    }

    // Delete parking without relationship constraints
    @Transactional
    public void deleteParking(Long id) {
        if (!parkingRepository.existsById(id)) {
            throw new RuntimeException("Parking not found with id: " + id);
        }

        Parking parking = parkingRepository.findById(id).orElseThrow();

        // Step 1: Find and handle assigned attendants
        List<User> assignedAttendants = userRepository.findByAssignedParking(parking);
        for (User attendant : assignedAttendants) {
            attendant.setAssignedParking(null);
            userRepository.save(attendant);
            logger.info("Unassigned attendant ID: {} from parking ID: {}", attendant.getId(), id);
        }

        // Step 2: Find and handle active vehicle entries
        List<VehicleEntry> vehicleEntries = vehicleEntryRepository.findByParking(parking);
        vehicleEntryRepository.deleteAll(vehicleEntries);
        logger.info("Deleted {} vehicle entries for parking ID: {}", vehicleEntries.size(), id);

        // Step 3: Find and handle reservations
        List<Reservation> reservations = reservationRepository.findByParking(parking);
        reservationRepository.deleteAll(reservations);
        logger.info("Deleted {} reservations for parking ID: {}", reservations.size(), id);

        // Step 4: Now delete the parking
        parkingRepository.deleteById(id);

        logger.info("Parking with ID {} and all related entities deleted successfully", id);
    }

    // Update available spots
    @Transactional
    public void updateAvailableSpots(Long parkingId, int newAvailableSpots) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            throw new RuntimeException("Parking not found with id: " + parkingId);
        }

        Parking parking = parkingOpt.get();

        if (newAvailableSpots < 0) {
            throw new RuntimeException("Available spots cannot be negative");
        }

        if (newAvailableSpots > parking.getTotalSpots()) {
            throw new RuntimeException("Available spots cannot exceed total spots");
        }

        parking.setAvailableSpots(newAvailableSpots);
        parkingRepository.save(parking);
    }

    // Temporary method to fix available spots
    @Transactional
    public void fixAvailableSpots(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isPresent()) {
            Parking parking = parkingOpt.get();
            if (parking.getAvailableSpots() > parking.getTotalSpots()) {
                logger.info("Fixing available spots for parking ID: {} (was {}/{})",
                        parkingId, parking.getAvailableSpots(), parking.getTotalSpots());

                parking.setAvailableSpots(parking.getTotalSpots());
                parkingRepository.save(parking);

                logger.info("Fixed available spots for parking ID: {} (now {}/{})",
                        parkingId, parking.getAvailableSpots(), parking.getTotalSpots());
            }
        }
    }
}