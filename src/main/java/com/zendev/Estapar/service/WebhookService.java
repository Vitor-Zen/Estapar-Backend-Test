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
import java.time.LocalTime;
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

    // Handles vehicle ENTRY event
    // Checks if the license plate already has an active entry (no duplicate entries)
    // Validates if there is at least one available and open sector at the given entry time
    // Creates and persists a new VehicleEntry with ENTERED status
    @Transactional
    private void handleEntry(WebhookRequest request) {
        if (vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate()).isPresent()) {
            throw new BusinessException("Vehicle already has an active entry for plate " + request.licensePlate(), HttpStatus.CONFLICT);
        }

        LocalDateTime entryTime = LocalDateTime.parse(
                request.entryTime(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        );
        LocalTime entryLocalTime = entryTime.toLocalTime();

        boolean hasAvailableAndOpenSector = sectorRepository.findAll()
                .stream()
                .filter(sector -> !sector.getIsFull())
                .anyMatch(sector -> {
                    LocalTime open  = LocalTime.parse(sector.getOpenHour());
                    LocalTime close = LocalTime.parse(sector.getCloseHour());
                    return !entryLocalTime.isBefore(open) && !entryLocalTime.isAfter(close);
                });

        if (!hasAvailableAndOpenSector) {
            throw new BusinessException("No available sector open at entry time " + entryLocalTime, HttpStatus.CONFLICT);
        }

        VehicleEntry entry = VehicleEntry.builder()
                .licensePlate(request.licensePlate())
                .entryTime(entryTime)
                .currency("BRL")
                .status(VehicleEntryStatus.ENTERED)
                .build();

        vehicleEntryRepository.save(entry);
    }

    // Handles vehicle PARKED event
    // Finds the spot by lat/lng coordinates and checks if it is available
    // Calculates the occupancy rate of the sector at the moment of parking
    // Marks the spot as occupied, updates sector fullness if needed
    // Associates the spot and sector to the vehicle entry and sets status to PARKED
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

    // Handles vehicle EXIT event
    // Finds the active entry for the given license plate
    // Frees the spot and marks the sector as not full
    // Validates if the vehicle exceeded the sector duration limit
    // Calculates the final price based on time parked and occupancy rate at entry
    // Persists exit time, price and sets status to EXITED
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

        LocalDateTime exitTime = LocalDateTime.parse(
                request.exitTime(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        );

        long minutesParked = ChronoUnit.MINUTES.between(entry.getEntryTime(), exitTime);
        if (sector.getDurationLimitMinutes() != null && minutesParked > sector.getDurationLimitMinutes()) {
            throw new BusinessException("Vehicle exceeded the duration limit of " + sector.getDurationLimitMinutes() + " minutes for sector " + sector.getSector(), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        entry.setExitTime(exitTime);

        BigDecimal price = calculatePrice(entry, sector);
        entry.setPrice(price);

        entry.setStatus(VehicleEntryStatus.EXITED);
        vehicleEntryRepository.save(entry);
    }

    // Calculates the parking price based on duration and occupancy rate at entry
    // First 30 minutes are free
    // After 30 minutes, charges a fixed rate per hour (rounded up) using the sector base price
    // Applies dynamic pricing: discount if low occupancy, surcharge if high occupancy
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