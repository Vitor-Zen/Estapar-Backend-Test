package com.zendev.Estapar.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sector;
    private Double basePrice;
    private Integer maxCapacity;
    private String openHour;
    private String closeHour;
    private Integer durationLimitMinutes;

    @Builder.Default
    private Boolean isFull = false;
}