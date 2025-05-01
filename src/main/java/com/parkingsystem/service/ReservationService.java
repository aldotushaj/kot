package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ParkingRepository parkingRepository;

    // Create a new reservation
    public Reservation createReservation(String licensePlate, LocalDateTime startTime,
                                         LocalDateTime endTime, Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isPresent()) {
            Parking parking = parkingOpt.get();

            // Check if there are available spots
            if (parking.getAvailableSpots() <= 0) {
                throw new RuntimeException("No available spots in this parking location");
            }

            // Calculate price
            BigDecimal price = calculatePrice(parking.getHourlyRate(), startTime, endTime);

            // Create reservation
            Reservation reservation = new Reservation(licensePlate, startTime, endTime, price, parking);

            // Update available spots
            parking.setAvailableSpots(parking.getAvailableSpots() - 1);
            parkingRepository.save(parking);

            return reservationRepository.save(reservation);
        } else {
            throw new RuntimeException("Parking location not found");
        }
    }

    // Calculate price based on duration and hourly rate
    private BigDecimal calculatePrice(BigDecimal hourlyRate, LocalDateTime startTime, LocalDateTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();
        if (Duration.between(startTime, endTime).toMinutes() % 60 > 0) {
            hours++; // Round up to the next hour
        }
        return hourlyRate.multiply(BigDecimal.valueOf(hours));
    }

    // Get reservation by id
    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    // Get all reservations for a parking location
    public List<Reservation> getReservationsByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);
        return parkingOpt.map(parking -> reservationRepository.findByParking(parking))
                .orElseThrow(() -> new RuntimeException("Parking not found"));
    }

    // Update payment status
    public Reservation updatePaymentStatus(Long reservationId, boolean isPaid) {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);

        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            reservation.setPaid(isPaid);
            return reservationRepository.save(reservation);
        } else {
            throw new RuntimeException("Reservation not found");
        }
    }

    // Cancel reservation
    public void cancelReservation(Long reservationId) {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);

        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            Parking parking = reservation.getParking();

            // Update available spots
            parking.setAvailableSpots(parking.getAvailableSpots() + 1);
            parkingRepository.save(parking);

            // Delete reservation
            reservationRepository.deleteById(reservationId);
        } else {
            throw new RuntimeException("Reservation not found");
        }
    }

    // Find reservations by license plate
    public List<Reservation> findReservationsByLicensePlate(String licensePlate) {
        return reservationRepository.findByLicensePlate(licensePlate);
    }
}