package com.parkingsystem.repository;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByParking(Parking parking);
    List<Reservation> findByParkingAndStartTimeBetween(Parking parking, LocalDateTime start, LocalDateTime end);
    List<Reservation> findByLicensePlate(String licensePlate);

    // Add this method to find reservations by username
    List<Reservation> findByUsername(String username);
}