package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.VehicleEntry;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.VehicleEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleEntryService {

    @Autowired
    private VehicleEntryRepository vehicleEntryRepository;

    @Autowired
    private ParkingRepository parkingRepository;

    // Register vehicle entry
    public VehicleEntry registerEntry(String licensePlate, Long parkingId, boolean reservedFromApp) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isPresent()) {
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
        } else {
            throw new RuntimeException("Parking location not found");
        }
    }

    // Register vehicle exit
    public VehicleEntry registerExit(Long entryId) {
        Optional<VehicleEntry> entryOpt = vehicleEntryRepository.findById(entryId);

        if (entryOpt.isPresent()) {
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
        } else {
            throw new RuntimeException("Vehicle entry not found");
        }
    }

    // Get all active entries for a parking location
    public List<VehicleEntry> getActiveEntriesByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        return parkingOpt.map(parking -> vehicleEntryRepository.findByParkingAndTimeOutIsNull(parking))
                .orElseThrow(() -> new RuntimeException("Parking not found"));
    }

    // Get all entries for a parking location
    public List<VehicleEntry> getAllEntriesByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        return parkingOpt.map(parking -> vehicleEntryRepository.findByParking(parking))
                .orElseThrow(() -> new RuntimeException("Parking not found"));
    }

    // Find active entries by license plate
    public List<VehicleEntry> findActiveEntriesByLicensePlate(String licensePlate) {
        return vehicleEntryRepository.findByLicensePlateAndTimeOutIsNull(licensePlate);
    }

    // Get vehicle entry by id
    public Optional<VehicleEntry> getVehicleEntryById(Long id) {
        return vehicleEntryRepository.findById(id);
    }

    // Helper method to get parking info from entry id
    public VehicleEntry getReservationsByEntryId(Long entryId) {
        return vehicleEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Vehicle entry not found"));
    }
}