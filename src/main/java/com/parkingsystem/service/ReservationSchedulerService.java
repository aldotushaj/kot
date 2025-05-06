// Create a new file: src/main/java/com/parkingsystem/service/ReservationSchedulerService.java
package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationSchedulerService {

    private final ReservationRepository reservationRepository;
    private final ParkingRepository parkingRepository;
    private final Logger logger = LoggerFactory.getLogger(ReservationSchedulerService.class);

    @Autowired
    public ReservationSchedulerService(ReservationRepository reservationRepository,
                                       ParkingRepository parkingRepository) {
        this.reservationRepository = reservationRepository;
        this.parkingRepository = parkingRepository;
    }


    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void updateExpiredReservations() {
        logger.info("Running expired reservations check");

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getEndTime() != null && r.getEndTime().isBefore(now))
                .filter(r -> !r.isProcessedExpiration())
                .collect(Collectors.toList());

        for (Reservation reservation : expiredReservations) {
            Parking parking = reservation.getParking();

            // Only increment if it won't exceed the total spots
            int newAvailableSpots = parking.getAvailableSpots() + 1;
            if (newAvailableSpots <= parking.getTotalSpots()) {
                parking.setAvailableSpots(newAvailableSpots);
                parkingRepository.save(parking);

                // Mark as processed
                reservation.setProcessedExpiration(true);
                reservationRepository.save(reservation);

                logger.info("Updated available spots for expired reservation ID: {}",
                        reservation.getId());
            } else {
                // Log an error if we would exceed total spots
                logger.error("Cannot update spots for parking ID: {} - would exceed total spots ({} > {})",
                        parking.getId(), newAvailableSpots, parking.getTotalSpots());

                // Still mark as processed to avoid retrying
                reservation.setProcessedExpiration(true);
                reservationRepository.save(reservation);
            }
        }
    }
}