package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.GarageResponse;
import com.zendev.Estapar.exception.BusinessException;
import com.zendev.Estapar.mapper.SpotMapper;
import com.zendev.Estapar.mapper.SectorMapper;
import com.zendev.Estapar.model.Spot;
import com.zendev.Estapar.repository.SectorRepository;
import com.zendev.Estapar.repository.SpotRepository;
import com.zendev.Estapar.repository.VehicleEntryRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GarageService {

    private final RestClient estaparRestClient;
    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;
    private final SectorMapper sectorMapper;
    private final SpotMapper spotMapper;
    private final VehicleEntryRepository vehicleEntryRepository;

    public GarageService(RestClient estaparRestClient, SectorRepository sectorRepository, SpotRepository spotRepository, SectorMapper sectorMapper, SpotMapper spotMapper, VehicleEntryRepository vehicleEntryRepository) {
        this.estaparRestClient = estaparRestClient;
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.sectorMapper = sectorMapper;
        this.spotMapper = spotMapper;
        this.vehicleEntryRepository = vehicleEntryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadGarageData() {
        GarageResponse response = estaparRestClient.get()
                .uri("/garage")
                .retrieve()
                .body(GarageResponse.class);

        if(response == null){
            throw new BusinessException("Failed to fetch garage data from simulator", HttpStatus.SERVICE_UNAVAILABLE);
        }

        vehicleEntryRepository.deleteAll();
        sectorRepository.deleteAll();
        spotRepository.deleteAll();

        response.garage().forEach(dto -> sectorRepository.save(sectorMapper.toEntity(dto)));
        response.spots().forEach(dto -> spotRepository.save(spotMapper.toEntity(dto)));

        // Check if sectors are already full based on initial data
        sectorRepository.findAll().forEach(sector -> {
            long occupiedSpots = spotRepository.findBySector(sector.getSector())
                    .stream()
                    .filter(Spot::getOccupied)
                    .count();
            if (occupiedSpots == sector.getMaxCapacity()) {
                sector.setIsFull(true);
                sectorRepository.save(sector);
            }
        });
    }
}