package com.zendev.Estapar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GarageResponse(
        List<SectorDto> garage,
        List<SpotsDto> spots
) {
    public record SectorDto(
            String sector,
            @JsonProperty("base_price") Double basePrice,
            @JsonProperty("max_capacity") Integer maxCapacity,
            @JsonProperty("open_hour") String openHour,
            @JsonProperty("close_hour") String closeHour,
            @JsonProperty("duration_limit_minutes") Integer durationLimitMinutes
    ) {}
    public record SpotsDto(
            Integer id,
            String sector,
            Double lat,
            Double lng,
            Boolean occupied
    ) {}
}
