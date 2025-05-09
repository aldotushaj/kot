package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.model.VehicleEntry;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.ReservationRepository;
import com.parkingsystem.repository.VehicleEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleEntryService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleEntryService.class);

    private final VehicleEntryRepository vehicleEntryRepository;
    private final ParkingRepository parkingRepository;
    private final ReservationRepository reservationRepository;

    @Autowired
    public VehicleEntryService(VehicleEntryRepository vehicleEntryRepository,
                               ParkingRepository parkingRepository,
                               ReservationRepository reservationRepository) {
        this.vehicleEntryRepository = vehicleEntryRepository;
        this.parkingRepository = parkingRepository;
        this.reservationRepository = reservationRepository;
    }

    // Register vehicle entry
    @Transactional
    public VehicleEntry registerEntry(String licensePlate, Long parkingId, boolean reservedFromApp) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            throw new RuntimeException("License plate is required");
        }

        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            throw new RuntimeException("Parking location not found");
        }

        Parking parking = parkingOpt.get();

        // Check if there are available spots (if not reserved)
        if (!reservedFromApp && parking.getAvailableSpots() <= 0) {
            throw new RuntimeException("No available spots in this parking location");
        }

        // Create entry
        VehicleEntry entry = new VehicleEntry(licensePlate, LocalDateTime.now(), parking, reservedFromApp);

        // Update available spots if not reserved from app
        // (reservations already update spots when created)
        if (!reservedFromApp) {
            parking.setAvailableSpots(parking.getAvailableSpots() - 1);
            parkingRepository.save(parking);
        }

        return vehicleEntryRepository.save(entry);
    }

    // Register vehicle exit
    @Transactional
    public VehicleEntry registerExit(Long entryId) {
        Optional<VehicleEntry> entryOpt = vehicleEntryRepository.findById(entryId);

        if (entryOpt.isEmpty()) {
            throw new RuntimeException("Vehicle entry not found");
        }

        VehicleEntry entry = entryOpt.get();

        // Check if already checked out
        if (entry.getTimeOut() != null) {
            throw new RuntimeException("Vehicle already checked out");
        }

        // Update timeOut to current time
        LocalDateTime exitTime = LocalDateTime.now();
        entry.setTimeOut(exitTime);

        // For walk-in vehicles (not reserved from app), we need to create a reservation record
        // to keep track of it in the history
        Reservation createdReservation = null;
        if (!entry.isReservedFromApp()) {
            // Create a reservation record for this walk-in entry
            try {
                LocalDateTime entryTime = entry.getTimeIn();
                String licensePlate = entry.getLicensePlate();
                Parking parking = entry.getParking();

                // Calculate price based on hourly rate and duration
                long minutes = Duration.between(entryTime, exitTime).toMinutes();
                long hours = minutes / 60;
                if (minutes % 60 > 0) {
                    hours++; // Round up to the next hour
                }
                hours = Math.max(1, hours); // Minimum 1 hour
                BigDecimal price = parking.getHourlyRate().multiply(BigDecimal.valueOf(hours));

                // Create a reservation record for tracking history
                Reservation reservation = new Reservation();
                reservation.setLicensePlate(licensePlate);
                reservation.setStartTime(entryTime);
                reservation.setEndTime(exitTime);
                reservation.setCalculatedPrice(price);
                reservation.setPaid(true); // Assuming walk-in vehicles pay at exit
                reservation.setReservedFromApp(false);
                reservation.setParking(parking);
                reservation.setUsername("walk-in"); // Special username for walk-ins
                reservation.setProcessedExpiration(true); // Already processed

                createdReservation = reservationRepository.save(reservation);
                logger.info("Created reservation record for walk-in vehicle: {}", licensePlate);
            } catch (Exception e) {
                logger.error("Failed to create reservation record for walk-in vehicle: {}", e.getMessage());
                // Don't throw exception here to avoid preventing checkout
            }
        }

        // Update available spots with validation
        Parking parking = entry.getParking();
        int newAvailableSpots = parking.getAvailableSpots() + 1;
        if (newAvailableSpots <= parking.getTotalSpots()) {
            parking.setAvailableSpots(newAvailableSpots);
            parkingRepository.save(parking);
        } else {
            logger.error("Cannot update spots for parking ID: {} - would exceed total spots", parking.getId());
            // Don't update spots if it would exceed total
        }

        // Save the updated entry
        VehicleEntry savedEntry = vehicleEntryRepository.save(entry);

        // Set the reservation ID for the controller to use
        if (createdReservation != null) {
            savedEntry.setCheckoutReservationId(createdReservation.getId());
        }

        return savedEntry;
    }

    // Get all active entries for a parking location
    public List<VehicleEntry> getActiveEntriesByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            throw new RuntimeException("Parking not found");
        }

        return vehicleEntryRepository.findByParkingAndTimeOutIsNull(parkingOpt.get());
    }

    // Get all entries for a parking location
    public List<VehicleEntry> getAllEntriesByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            throw new RuntimeException("Parking not found");
        }

        return vehicleEntryRepository.findByParking(parkingOpt.get());
    }

    // Find active entries by license plate
    public List<VehicleEntry> findActiveEntriesByLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            throw new RuntimeException("License plate is required");
        }

        return vehicleEntryRepository.findByLicensePlateAndTimeOutIsNull(licensePlate);
    }

    // Get vehicle entry by id
    public Optional<VehicleEntry> getVehicleEntryById(Long id) {
        return vehicleEntryRepository.findById(id);
    }

    // Helper method to get vehicle entry by ID - renamed for clarity
    public VehicleEntry getVehicleEntryByEntryId(Long entryId) {
        if (entryId == null) {
            throw new RuntimeException("Entry ID is required");
        }

        return vehicleEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Vehicle entry not found"));
    }
}