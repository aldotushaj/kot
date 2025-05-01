package com.parkingsystem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
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

    @ManyToOne
    private Parking parking;


    public Reservation(String licensePlate, LocalDateTime startTime, LocalDateTime endTime, BigDecimal price, boolean b, Parking parking) {
    }

    public Reservation() {

    }
}