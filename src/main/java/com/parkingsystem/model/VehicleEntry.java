package com.parkingsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class VehicleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String licensePlate;
    private LocalDateTime timeIn;
    private LocalDateTime timeOut;
    private boolean reservedFromApp;

    @ManyToOne
    private Parking parking;

    // Constructors
    public VehicleEntry() {}

    public VehicleEntry(String licensePlate, LocalDateTime timeIn, Parking parking, boolean reservedFromApp) {
        this.licensePlate = licensePlate;
        this.timeIn = timeIn;
        this.parking = parking;
        this.reservedFromApp = reservedFromApp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalDateTime timeIn) {
        this.timeIn = timeIn;
    }

    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(LocalDateTime timeOut) {
        this.timeOut = timeOut;
    }

    public boolean isReservedFromApp() {
        return reservedFromApp;
    }

    public void setReservedFromApp(boolean reservedFromApp) {
        this.reservedFromApp = reservedFromApp;
    }

    public Parking getParking() {
        return parking;
    }

    public void setParking(Parking parking) {
        this.parking = parking;
    }
}
