package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.repository.ParkingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ParkingService {

    @Autowired
    private ParkingRepository parkingRepository;

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
    public Parking createParking(String location, int totalSpots, BigDecimal hourlyRate) {
        Parking parking = new Parking(location, totalSpots, hourlyRate);
        return parkingRepository.save(parking);
    }

    // Update parking info
    public Parking updateParking(Parking parking) {
        return parkingRepository.save(parking);
    }

    // Delete parking
    public void deleteParking(Long id) {
        parkingRepository.deleteById(id);
    }

    // Update available spots
    public void updateAvailableSpots(Long parkingId, int newAvailableSpots) {
        parkingRepository.findById(parkingId).ifPresent(parking -> {
            parking.setAvailableSpots(newAvailableSpots);
            parkingRepository.save(parking);
        });
    }
}