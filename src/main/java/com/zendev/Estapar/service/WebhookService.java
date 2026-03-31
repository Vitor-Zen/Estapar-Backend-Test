package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.WebhookRequest;
import com.zendev.Estapar.enums.VehicleEntryStatus;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    public void processEvent(WebhookRequest request) {
        switch (request.eventType()) {
            case ENTRY -> handleEntry(request);
            case PARKED -> handleParked(request);
            case EXIT -> handleExit(request);
        }
    }

    @Transactional
    private void handleEntry(WebhookRequest request) {
        if (vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate()).isPresent()) {
            throw new BusinessException("Vehicle already has an active entry for plate " + request.licensePlate(), HttpStatus.CONFLICT);
        }

        boolean hasAvailableSpot = sectorRepository.findAll()
                .stream()
                .anyMatch(sector -> !sector.getIsFull());

        if (!hasAvailableSpot) {
            throw new BusinessException("Garage is full, no available spots", HttpStatus.CONFLICT);
        }

        VehicleEntry entry = VehicleEntry.builder()
                .licensePlate(request.licensePlate())
                .entryTime(LocalDateTime.parse(request.entryTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")))
                .currency("BRL")
                .status(VehicleEntryStatus.ENTERED)
                .build();

        vehicleEntryRepository.save(entry);
    }

    @Transactional
    private void handleParked(WebhookRequest request) {
        Spot spot = spotRepository.findByLatAndLng(request.lat(), request.lng())
                .orElseThrow(() -> new BusinessException("Spot not found for given location", HttpStatus.NOT_FOUND));

        if (spot.getOccupied()) {
            throw new BusinessException("Spot is already occupied", HttpStatus.CONFLICT);
        }

        VehicleEntry entry = vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate())
                .orElseThrow(() -> new BusinessException("Vehicle entry not found for plate " + request.licensePlate(), HttpStatus.NOT_FOUND));

        Sector sector = sectorRepository.findBySector(spot.getSector())
                .orElseThrow(() -> new BusinessException("Sector not found: " + spot.getSector(), HttpStatus.NOT_FOUND));

        // Calculate current sector occupancy rate
        long occupiedSpots = spotRepository.findBySector(spot.getSector())
                .stream()
                .filter(Spot::getOccupied)
                .count();

        double occupancyRateAtEntry = (double) occupiedSpots / sector.getMaxCapacity() * 100;

        if (occupiedSpots + 1 == sector.getMaxCapacity()) {
            sector.setIsFull(true);
            sectorRepository.save(sector);
        }

        spot.setOccupied(true);
        spotRepository.save(spot);

        entry.setSpotId(spot.getId());
        entry.setSector(spot.getSector());
        entry.setOccupancyRateAtEntry(occupancyRateAtEntry);
        entry.setStatus(VehicleEntryStatus.PARKED);
        vehicleEntryRepository.save(entry);
    }

    @Transactional
    private void handleExit(WebhookRequest request) {
        VehicleEntry entry = vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate())
                .orElseThrow(() -> new BusinessException("Vehicle entry not found for plate " + request.licensePlate(), HttpStatus.NOT_FOUND));

        Spot spot = spotRepository.findById(entry.getSpotId())
                .orElseThrow(() -> new BusinessException("Spot not found " + entry.getSpotId(), HttpStatus.NOT_FOUND));

        spot.setOccupied(false);
        spotRepository.save(spot);

        Sector sector = sectorRepository.findBySector(entry.getSector())
                .orElseThrow(() -> new BusinessException("Sector not found " + entry.getSector(), HttpStatus.NOT_FOUND));

        sector.setIsFull(false);
        sectorRepository.save(sector);

        LocalDateTime exitTime = LocalDateTime.parse(request.exitTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        entry.setExitTime(exitTime);

        BigDecimal price = calculatePrice(entry, sector);
        entry.setPrice(price);

        entry.setStatus(VehicleEntryStatus.EXITED);
        vehicleEntryRepository.save(entry);
    }

    private BigDecimal calculatePrice(VehicleEntry entry, Sector sector){
        // Calculate parking duration in minutes
        long minutes = ChronoUnit.MINUTES.between(entry.getEntryTime(), entry.getExitTime());

        if (minutes <= 30){
            return BigDecimal.ZERO;
        }

        long hours = (long) Math.ceil(minutes / 60.0);

        // Apply basePrice according to the sector
        BigDecimal price = BigDecimal.valueOf(hours)
                .multiply(BigDecimal.valueOf(sector.getBasePrice()));

        // Apply dynamic pricing multiplier based on occupancy rate at entry
        double occupancy = entry.getOccupancyRateAtEntry();

        if(occupancy < 25) {
            price = price.multiply(BigDecimal.valueOf(0.9)); // 10% discount
        } else if (occupancy < 50) {
            price = price.multiply(BigDecimal.ONE); // normal price
        } else if (occupancy < 75) {
            price = price.multiply(BigDecimal.valueOf(1.1)); // 10% increase
        } else {
            price = price.multiply(BigDecimal.valueOf(1.25)); // 25% increase
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }
}