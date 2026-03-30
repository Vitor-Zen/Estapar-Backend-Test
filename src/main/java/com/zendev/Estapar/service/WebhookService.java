package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.WebhookRequest;
import com.zendev.Estapar.exception.BusinessException;
import com.zendev.Estapar.model.Sector;
import com.zendev.Estapar.model.Spot;
import com.zendev.Estapar.model.VehicleEntry;
import com.zendev.Estapar.repository.SectorRepository;
import com.zendev.Estapar.repository.SpotRepository;
import com.zendev.Estapar.repository.VehicleEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WebhookService {

    private final VehicleEntryRepository vehicleEntryRepository;
    private final SpotRepository spotRepository;
    private final SectorRepository sectorRepository;

    public WebhookService(VehicleEntryRepository vehicleEntryRepository, SpotRepository spotRepository, SectorRepository sectorRepository) {
        this.vehicleEntryRepository = vehicleEntryRepository;
        this.spotRepository = spotRepository;
        this.sectorRepository = sectorRepository;
    }

    public void processEvent(WebhookRequest request){
        switch (request.eventType()){
            case ENTRY -> handleEntry(request);
            case PARKED -> handleParked(request);
            case EXIT -> handleExit(request);
        }
    }

    private void handleEntry(WebhookRequest request){

        // Check if any sector is available
        boolean hasAvailableSpot = sectorRepository.findAll()
                .stream()
                .anyMatch(sector -> !sector.getIsFull());

        if (!hasAvailableSpot) {
            throw new BusinessException("Garage is full, no available spots", HttpStatus.CONFLICT);
        }

        VehicleEntry entry = VehicleEntry.builder()
                .licensePlate(request.licensePlate())
                .entryTime(LocalDateTime.parse(request.entryTime(),DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )))
                .currency("BRL")
                .build();

        vehicleEntryRepository.save(entry);
    }

    @Transactional
    private void handleParked(WebhookRequest request){
        Spot spot = spotRepository.findByLatAndLng(request.lat(), request.lng())
                .orElseThrow(() -> new BusinessException("Spot not found for given location", HttpStatus.NOT_FOUND));

        if(spot.getOccupied()){
            throw new BusinessException("Spot is already occupied", HttpStatus.CONFLICT);
        }

        spot.setOccupied(true);
        spotRepository.save(spot);

        VehicleEntry entry = vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate())
                .orElseThrow(() -> new BusinessException("Vehicle entry not found for plate" + request.licensePlate(), HttpStatus.NOT_FOUND));

        entry.setSpotId(spot.getId());
        entry.setSector(spot.getSector());

        // Check if sector exists and get its configurations
        Sector sector = sectorRepository.findBySector(spot.getSector())
                .orElseThrow(() -> new BusinessException("Sector not found: " + spot.getSector(), HttpStatus.NOT_FOUND));

        long occupiedSpots = spotRepository.findBySector(spot.getSector())
                .stream()
                .filter(Spot::getOccupied)
                .count();

        double occupancyRate = (double) occupiedSpots / sector.getMaxCapacity() * 100;
        entry.setOccupancyRateAtEntry(occupancyRate);
        vehicleEntryRepository.save(entry);

        if(occupiedSpots == sector.getMaxCapacity()){
            sector.setIsFull(true);
            sectorRepository.save(sector);
        }
    }

    private void handleExit(WebhookRequest request){}
}