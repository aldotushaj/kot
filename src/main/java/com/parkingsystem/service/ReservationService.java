package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ParkingRepository parkingRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              ParkingRepository parkingRepository) {
        this.reservationRepository = reservationRepository;
        this.parkingRepository = parkingRepository;
    }

    // Create a new reservation
    @Transactional
    public Reservation createReservation(String licensePlate, LocalDateTime startTime,
                                         LocalDateTime endTime, Long parkingId) {
        logger.info("Creating reservation for license plate: {}, parking ID: {}, start: {}, end: {}",
                licensePlate, parkingId, startTime, endTime);

        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            logger.error("License plate is empty or null");
            throw new RuntimeException("License plate cannot be empty");
        }

        if (startTime == null || endTime == null) {
            logger.error("Start time or end time is null");
            throw new RuntimeException("Start and end times must be provided");
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            logger.error("End time {} is before or equal to start time {}", endTime, startTime);
            throw new RuntimeException("End time must be after start time");
        }

        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            logger.error("Parking with id {} not found", parkingId);
            throw new RuntimeException("Parking location not found");
        }

        Parking parking = parkingOpt.get();
        logger.info("Found parking: {}, available spots: {}", parking.getLocation(), parking.getAvailableSpots());

        // Check if there are available spots - but do not change them yet
        if (parking.getAvailableSpots() <= 0) {
            logger.error("No available spots in parking {}", parking.getLocation());
            throw new RuntimeException("No available spots in this parking location");
        }

        // Calculate price
        BigDecimal price = calculatePrice(parking.getHourlyRate(), startTime, endTime);
        logger.info("Calculated price: {}", price);

        try {
            // Create reservation with the builder pattern
            Reservation reservation = Reservation.builder()
                    .licensePlate(licensePlate)
                    .startTime(startTime)
                    .endTime(endTime)
                    .calculatedPrice(price)
                    .isPaid(false)
                    .reservedFromApp(true)
                    .parking(parking)
                    .build();

            // Save the reservation without changing available spots
            Reservation savedReservation = reservationRepository.save(reservation);
            logger.info("Reservation created with ID: {}", savedReservation.getId());

            return savedReservation;
        } catch (Exception e) {
            logger.error("Error creating reservation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create reservation: " + e.getMessage());
        }
    }
    // Calculate price based on duration and hourly rate
    private BigDecimal calculatePrice(BigDecimal hourlyRate, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        long hours = minutes / 60;

        // Round up to the next hour if there are remaining minutes
        if (minutes % 60 > 0) {
            hours++;
        }

        // Ensure at least 1 hour is charged
        hours = Math.max(1, hours);

        logger.info("Calculating price for {} hours at rate {}", hours, hourlyRate);
        return hourlyRate.multiply(BigDecimal.valueOf(hours));
    }

    // Get reservation by id
    public Optional<Reservation> getReservationById(Long id) {
        logger.info("Fetching reservation with ID: {}", id);
        return reservationRepository.findById(id);
    }

    // Get all reservations for a parking location
    public List<Reservation> getReservationsByParking(Long parkingId) {
        logger.info("Fetching reservations for parking ID: {}", parkingId);
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            logger.error("Parking with ID {} not found", parkingId);
            throw new RuntimeException("Parking not found");
        }

        return reservationRepository.findByParking(parkingOpt.get());
    }

    // Get reservations for a parking within a specific time range
    public List<Reservation> getReservationsByParkingAndTimeRange(Long parkingId,
                                                                  LocalDateTime start,
                                                                  LocalDateTime end) {
        logger.info("Fetching reservations for parking ID: {} between {} and {}",
                parkingId, start, end);

        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            logger.error("Parking with ID {} not found", parkingId);
            throw new RuntimeException("Parking not found");
        }

        return reservationRepository.findByParkingAndStartTimeBetween(parkingOpt.get(), start, end);
    }

    // Update payment status
    @Transactional
    public Reservation updatePaymentStatus(Long reservationId, boolean isPaid) {
        logger.info("Updating payment status for reservation ID: {} to paid: {}", reservationId, isPaid);
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);

        if (reservationOpt.isEmpty()) {
            logger.error("Reservation with ID {} not found", reservationId);
            throw new RuntimeException("Reservation not found");
        }

        Reservation reservation = reservationOpt.get();

        // Only update available spots if the reservation is being marked as paid
        // and wasn't previously paid
        if (isPaid && !reservation.isPaid()) {
            Parking parking = reservation.getParking();

            // Decrement available spots
            if (parking.getAvailableSpots() > 0) {
                parking.setAvailableSpots(parking.getAvailableSpots() - 1);
                parkingRepository.save(parking);
                logger.info("Updated available spots to: {} after payment", parking.getAvailableSpots());
            } else {
                logger.warn("No available spots to decrement for parking ID: {}", parking.getId());
                // You might want to handle this case differently - potentially notify admin
            }
        }

        reservation.setPaid(isPaid);
        return reservationRepository.save(reservation);
    }
    // Cancel reservation
    @Transactional
    public void cancelReservation(Long reservationId) {
        logger.info("Cancelling reservation with ID: {}", reservationId);
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);

        if (reservationOpt.isEmpty()) {
            logger.error("Reservation with ID {} not found", reservationId);
            throw new RuntimeException("Reservation not found");
        }

        Reservation reservation = reservationOpt.get();
        Parking parking = reservation.getParking();

        // Update available spots - with validation check
        int newAvailableSpots = parking.getAvailableSpots() + 1;
        if (newAvailableSpots <= parking.getTotalSpots()) {
            parking.setAvailableSpots(newAvailableSpots);
            parkingRepository.save(parking);
            logger.info("Updated available spots to: {}", parking.getAvailableSpots());
        } else {
            logger.error("Cannot update spots for parking ID: {} - would exceed total spots", parking.getId());
        }

        // Delete reservation
        reservationRepository.deleteById(reservationId);
        logger.info("Reservation deleted successfully");
    }
    // Find reservations by license plate
    public List<Reservation> findReservationsByLicensePlate(String licensePlate) {
        logger.info("Finding reservations for license plate: {}", licensePlate);
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            logger.error("License plate is empty or null");
            throw new RuntimeException("License plate cannot be empty");
        }

        try {
            return reservationRepository.findByLicensePlate(licensePlate);
        } catch (Exception e) {
            logger.error("Error finding reservations by license plate: {}", e.getMessage(), e);
            // Return empty list instead of throwing to avoid error in dashboard
            return new ArrayList<>();
        }
    }

    // Get all reservations
    public List<Reservation> getAllReservations() {
        logger.info("Fetching all reservations");
        return reservationRepository.findAll();
    }

    // Find upcoming reservations (reservations that start in the future)
    public List<Reservation> findUpcomingReservations() {
        logger.info("Finding upcoming reservations");
        LocalDateTime now = LocalDateTime.now();
        try {
            List<Reservation> allReservations = reservationRepository.findAll();
            return allReservations.stream()
                    .filter(r -> r.getStartTime().isAfter(now))
                    .sorted((r1, r2) -> r1.getStartTime().compareTo(r2.getStartTime()))
                    .toList();
        } catch (Exception e) {
            logger.error("Error finding upcoming reservations: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Find active reservations (reservations that have started but not ended)
    public List<Reservation> findActiveReservations() {
        logger.info("Finding active reservations");
        LocalDateTime now = LocalDateTime.now();
        try {
            List<Reservation> allReservations = reservationRepository.findAll();
            return allReservations.stream()
                    .filter(r -> r.getStartTime().isBefore(now) && r.getEndTime().isAfter(now))
                    .toList();
        } catch (Exception e) {
            logger.error("Error finding active reservations: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Find past reservations (reservations that have ended)
    public List<Reservation> findPastReservations() {
        logger.info("Finding past reservations");
        LocalDateTime now = LocalDateTime.now();
        try {
            List<Reservation> allReservations = reservationRepository.findAll();
            return allReservations.stream()
                    .filter(r -> r.getEndTime().isBefore(now))
                    .sorted((r1, r2) -> r2.getEndTime().compareTo(r1.getEndTime())) // Most recent first
                    .toList();
        } catch (Exception e) {
            logger.error("Error finding past reservations: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


}