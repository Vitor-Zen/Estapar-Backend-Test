package com.zendev.Estapar.model;

import com.zendev.Estapar.dto.GarageResponse;
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

    public static Sector from(GarageResponse.SectorDto dto){
        return Sector.builder()
                .sector(dto.sector())
                .basePrice(dto.basePrice())
                .maxCapacity(dto.maxCapacity())
                .openHour(dto.openHour())
                .closeHour(dto.closeHour())
                .durationLimitMinutes(dto.durationLimitMinutes())
                .build();
    }
}