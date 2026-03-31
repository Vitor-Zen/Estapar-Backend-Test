package com.zendev.Estapar.model;

import com.zendev.Estapar.enums.VehicleEntryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String licensePlate;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer spotId;
    private String sector;
    private BigDecimal price;
    private String currency;
    private Double occupancyRateAtEntry;
    @Enumerated(EnumType.STRING)
    private VehicleEntryStatus status;
}