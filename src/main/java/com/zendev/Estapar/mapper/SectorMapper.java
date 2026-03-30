package com.zendev.Estapar.mapper;

import com.zendev.Estapar.dto.GarageResponse;
import com.zendev.Estapar.model.Sector;
import org.springframework.stereotype.Component;

@Component
public class SectorMapper {
    public Sector toEntity(GarageResponse.SectorDto dto) {
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
