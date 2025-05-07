package com.parkingsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String licensePlate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal calculatedPrice;
    private boolean isPaid;
    private boolean reservedFromApp;
    private boolean processedExpiration = false;

    // Add this field to track who made the reservation
    private String username;

    @ManyToOne
    private Parking parking;

    // Constructor with all necessary fields
    public Reservation(String licensePlate, LocalDateTime startTime, LocalDateTime endTime,
                       BigDecimal calculatedPrice, boolean isPaid, Parking parking, String username) {
        this.licensePlate = licensePlate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.calculatedPrice = calculatedPrice;
        this.isPaid = isPaid;
        this.reservedFromApp = true;
        this.parking = parking;
        this.username = username;
    }
}