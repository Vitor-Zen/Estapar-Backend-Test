package com.zendev.Estapar.model;

import com.zendev.Estapar.dto.GarageResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "spots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spot {

    @Id
    private Integer id;

    private String sector;
    private Double lat;
    private Double lng;
    private Boolean occupied;
}