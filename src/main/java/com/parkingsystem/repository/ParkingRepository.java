package com.parkingsystem.repository;

import com.parkingsystem.model.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingRepository extends JpaRepository<Parking, Long> {
    // Custom query methods if needed
    List<Parking> findByAvailableSpotsGreaterThan(int spots);
}
