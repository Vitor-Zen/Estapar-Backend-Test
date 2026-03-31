package com.zendev.Estapar.mapper;

import com.zendev.Estapar.dto.GarageResponse;
import com.zendev.Estapar.model.Spot;
import org.springframework.stereotype.Component;

@Component
public class SpotMapper {
    public Spot toEntity(GarageResponse.SpotsDto dto){
        return Spot.builder()
                .id(dto.id())
                .sector(dto.sector())
                .lat(dto.lat())
                .lng(dto.lng())
                .occupied(dto.occupied())
                .build();
    }
}
