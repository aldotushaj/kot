package com.parkingsystem.repository;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.VehicleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface VehicleEntryRepository extends JpaRepository<VehicleEntry, Long> {
    List<VehicleEntry> findByParking(Parking parking);
    List<VehicleEntry> findByParkingAndTimeOutIsNull(Parking parking);
    List<VehicleEntry> findByLicensePlateAndTimeOutIsNull(String licensePlate);
}