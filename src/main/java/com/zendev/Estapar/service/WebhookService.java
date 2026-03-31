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

import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Transactional
    private void handleEntry(WebhookRequest request){
        if (vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate()).isPresent()) {
            throw new BusinessException("Vehicle already has an active entry for plate " + request.licensePlate(), HttpStatus.CONFLICT);
        }

        List<Sector> sectors = sectorRepository.findAllForUpdate();
        Sector reservedSector = sectors.stream()
                .filter(this::hasAvailableReservation)
                .max(Comparator.comparingInt(this::availableReservationSlots))
                .orElseThrow(() -> new BusinessException("Garage is full, no available spots", HttpStatus.CONFLICT));

        reservedSector.setReservedCount(reservedSector.getReservedCount() + 1);
        refreshFullFlag(reservedSector);
        sectorRepository.save(reservedSector);

        double occupancyRateAtEntry = (double) reservedSector.getReservedCount() / reservedSector.getMaxCapacity() * 100;

        VehicleEntry entry = VehicleEntry.builder()
                .licensePlate(request.licensePlate())
                .entryTime(LocalDateTime.parse(request.entryTime(),DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )))
                .currency("BRL")
                .reservedSector(reservedSector.getSector())
                .sector(reservedSector.getSector())
                .status(VehicleEntryStatus.ENTERED)
                .occupancyRateAtEntry(occupancyRateAtEntry)
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

        VehicleEntry entry = vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull(request.licensePlate())
                .orElseThrow(() -> new BusinessException("Vehicle entry not found for plate " + request.licensePlate(), HttpStatus.NOT_FOUND));

        Sector parkedSector = sectorRepository.findBySectorForUpdate(spot.getSector())
                .orElseThrow(() -> new BusinessException("Sector not found: " + spot.getSector(), HttpStatus.NOT_FOUND));

        if (entry.getReservedSector() != null && !entry.getReservedSector().equals(spot.getSector())) {
            Sector oldReservedSector = sectorRepository.findBySectorForUpdate(entry.getReservedSector())
                    .orElseThrow(() -> new BusinessException("Sector not found: " + entry.getReservedSector(), HttpStatus.NOT_FOUND));

            if (!hasAvailableReservation(parkedSector)) {
                throw new BusinessException("Target sector is full for new reservations", HttpStatus.CONFLICT);
            }

            oldReservedSector.setReservedCount(Math.max(0, oldReservedSector.getReservedCount() - 1));
            refreshFullFlag(oldReservedSector);
            sectorRepository.save(oldReservedSector);

            parkedSector.setReservedCount(parkedSector.getReservedCount() + 1);
            refreshFullFlag(parkedSector);
            sectorRepository.save(parkedSector);
        }

        spot.setOccupied(true);
        spotRepository.save(spot);

        entry.setSpotId(spot.getId());
        entry.setSector(spot.getSector());
        entry.setReservedSector(spot.getSector());
        entry.setStatus(VehicleEntryStatus.PARKED);
        vehicleEntryRepository.save(entry);
    }

    private void handleExit(WebhookRequest request){}

    private boolean hasAvailableReservation(Sector sector) {
        return availableReservationSlots(sector) > 0;
    }

    private int availableReservationSlots(Sector sector) {
        int reserved = sector.getReservedCount() == null ? 0 : sector.getReservedCount();
        return sector.getMaxCapacity() - reserved;
    }

    private void refreshFullFlag(Sector sector) {
        sector.setIsFull(!hasAvailableReservation(sector));
    }

}