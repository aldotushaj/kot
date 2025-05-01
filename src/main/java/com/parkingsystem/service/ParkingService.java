package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.repository.ParkingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ParkingService {

    private final ParkingRepository parkingRepository;

    @Autowired
    public ParkingService(ParkingRepository parkingRepository) {
        this.parkingRepository = parkingRepository;
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
    public Parking createParking(String location, int totalSpots, BigDecimal hourlyRate) {
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

    // Delete parking
    @Transactional
    public void deleteParking(Long id) {
        if (!parkingRepository.existsById(id)) {
            throw new RuntimeException("Parking not found with id: " + id);
        }

        parkingRepository.deleteById(id);
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
}