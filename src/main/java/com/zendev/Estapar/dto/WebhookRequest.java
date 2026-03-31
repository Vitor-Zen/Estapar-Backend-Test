package com.zendev.Estapar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zendev.Estapar.enums.EventType;

public record WebhookRequest(
        @JsonProperty("license_plate") String licensePlate,
        @JsonProperty("entry_time") String entryTime,
        @JsonProperty("exit_time") String exitTime,
        @JsonProperty("event_type")EventType eventType,
        Double lat,
        Double lng
        ) {
}