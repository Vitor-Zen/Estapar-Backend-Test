package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.GarageResponse;
import com.zendev.Estapar.mapper.SpotMapper;
import com.zendev.Estapar.mapper.SectorMapper;
import com.zendev.Estapar.repository.SectorRepository;
import com.zendev.Estapar.repository.SpotRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GarageService {

    private final RestClient estaparRestClient;
    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;
    private final SectorMapper sectorMapper;
    private final SpotMapper spotMapper;

    public GarageService(RestClient estaparRestClient, SectorRepository sectorRepository, SpotRepository spotRepository, SectorMapper sectorMapper, SpotMapper spotMapper) {
        this.estaparRestClient = estaparRestClient;
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.sectorMapper = sectorMapper;
        this.spotMapper = spotMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadGarageData() {
        GarageResponse response = estaparRestClient.get()
                .uri("/garage")
                .retrieve()
                .body(GarageResponse.class);

        if(response == null){
            throw new RuntimeException("Failed to fetch garage data from simulator");
        }

        sectorRepository.deleteAll();
        spotRepository.deleteAll();

        response.garage().forEach(dto -> sectorRepository.save(sectorMapper.toEntity(dto)));
        response.spots().forEach(dto -> spotRepository.save(spotMapper.toEntity(dto)));
    }
}