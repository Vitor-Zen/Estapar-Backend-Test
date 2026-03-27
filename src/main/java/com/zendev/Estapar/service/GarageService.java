package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.GarageResponse;
import com.zendev.Estapar.model.Sector;
import com.zendev.Estapar.model.Spot;
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

    public GarageService(RestClient estaparRestClient, SectorRepository sectorRepository, SpotRepository spotRepository) {
        this.estaparRestClient = estaparRestClient;
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
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

        response.garage().forEach(dto -> sectorRepository.save(Sector.from(dto)));
        response.spots().forEach(dto -> spotRepository.save(Spot.from(dto)));
    }
}