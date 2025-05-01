package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.VehicleEntry;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.VehicleEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleEntryService {

    private final VehicleEntryRepository vehicleEntryRepository;
    private final ParkingRepository parkingRepository;

    @Autowired
    public VehicleEntryService(VehicleEntryRepository vehicleEntryRepository,
                               ParkingRepository parkingRepository) {
        this.vehicleEntryRepository = vehicleEntryRepository;
        this.parkingRepository = parkingRepository;
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

        // Update timeOut
        entry.setTimeOut(LocalDateTime.now());

        // Update available spots
        Parking parking = entry.getParking();
        parking.setAvailableSpots(parking.getAvailableSpots() + 1);
        parkingRepository.save(parking);

        return vehicleEntryRepository.save(entry);
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