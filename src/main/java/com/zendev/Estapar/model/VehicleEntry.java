package com.zendev.Estapar.model;

import jakarta.persistence.*;
import lombok.*;

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
    private String entryTime;
    private String exitTime;
    private Integer spotId;
    private String sector;
    private Double price;
    private String currency;
    private Double occupancyRateAtEntry;
}