package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.WebhookRequest;
import com.zendev.Estapar.enums.EventType;
import com.zendev.Estapar.enums.VehicleEntryStatus;
import com.zendev.Estapar.exception.BusinessException;
import com.zendev.Estapar.model.Sector;
import com.zendev.Estapar.model.Spot;
import com.zendev.Estapar.model.VehicleEntry;
import com.zendev.Estapar.repository.SectorRepository;
import com.zendev.Estapar.repository.SpotRepository;
import com.zendev.Estapar.repository.VehicleEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Habilita o Mockito nesse teste — permite usar @Mock e @InjectMocks sem subir o Spring
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private VehicleEntryRepository vehicleEntryRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SectorRepository sectorRepository;

    @InjectMocks
    private WebhookService webhookService;

    private Sector sectorA;
    private Spot spot;
    private VehicleEntry activeEntry;

    @BeforeEach
    void setUp() {
        // Setor A aberto o dia todo com preço base 40.50
        sectorA = Sector.builder()
                .id(1L)
                .sector("A")
                .basePrice(40.5)
                .maxCapacity(10)
                .openHour("00:00")
                .closeHour("23:59")
                .durationLimitMinutes(1440)
                .isFull(false)
                .build();

        // Vaga disponível no setor A
        spot = Spot.builder()
                .id(1)
                .sector("A")
                .lat(-23.561684)
                .lng(-46.655981)
                .occupied(false)
                .build();

        // Entrada ativa de um veículo — sem exitTime (ainda estacionado)
        activeEntry = VehicleEntry.builder()
                .id(1L)
                .licensePlate("ZUL0001")
                .entryTime(LocalDateTime.of(2025, 1, 1, 10, 0))
                .spotId(1)
                .sector("A")
                .occupancyRateAtEntry(20.0)
                .status(VehicleEntryStatus.PARKED)
                .currency("BRL")
                .build();
    }

    // -------------------------
    // handleEntry
    // -------------------------

    @Test
    void handleEntry_shouldThrowException_whenVehicleAlreadyHasActiveEntry() {
        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0001"))
                .thenReturn(Optional.of(activeEntry));

        WebhookRequest request = new WebhookRequest(
                "ZUL0001", "2025-01-01T10:00:00.000Z", null, EventType.ENTRY, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> webhookService.processEvent(request));

        assertTrue(ex.getMessage().contains("ZUL0001"));
    }

    @Test
    void handleEntry_shouldThrowException_whenNoSectorIsOpenAtEntryTime() {
        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0002"))
                .thenReturn(Optional.empty());

        Sector closedSector = Sector.builder()
                .sector("B")
                .openHour("08:00")
                .closeHour("23:59")
                .isFull(false)
                .build();

        when(sectorRepository.findAll()).thenReturn(List.of(closedSector));

        WebhookRequest request = new WebhookRequest(
                "ZUL0002", "2025-01-01T06:00:00.000Z", null, EventType.ENTRY, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> webhookService.processEvent(request));

        assertTrue(ex.getMessage().contains("entry time"));
    }

    @Test
    void handleEntry_shouldSaveEntry_whenSectorIsOpenAndAvailable() {
        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0003"))
                .thenReturn(Optional.empty());

        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));

        WebhookRequest request = new WebhookRequest(
                "ZUL0003", "2025-01-01T10:00:00.000Z", null, EventType.ENTRY, null, null
        );

        assertDoesNotThrow(() -> webhookService.processEvent(request));

        verify(vehicleEntryRepository, times(1)).save(any(VehicleEntry.class));
    }

    // -------------------------
    // handleParked
    // -------------------------

    @Test
    void handleParked_shouldThrowException_whenSpotIsAlreadyOccupied() {
        Spot occupiedSpot = Spot.builder()
                .id(1)
                .sector("A")
                .lat(-23.561684)
                .lng(-46.655981)
                .occupied(true)
                .build();

        when(spotRepository.findByLatAndLng(-23.561684, -46.655981))
                .thenReturn(Optional.of(occupiedSpot));

        WebhookRequest request = new WebhookRequest(
                "ZUL0001", null, null, EventType.PARKED, -23.561684, -46.655981
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> webhookService.processEvent(request));

        assertTrue(ex.getMessage().contains("already occupied"));
    }

    @Test
    void handleParked_shouldMarkSpotAsOccupiedAndAssociateEntry() {
        when(spotRepository.findByLatAndLng(-23.561684, -46.655981))
                .thenReturn(Optional.of(spot));

        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0001"))
                .thenReturn(Optional.of(activeEntry));

        when(sectorRepository.findBySector("A"))
                .thenReturn(Optional.of(sectorA));

        when(spotRepository.findBySector("A"))
                .thenReturn(List.of(
                        Spot.builder().occupied(true).build(),
                        Spot.builder().occupied(true).build(),
                        Spot.builder().occupied(false).build()
                ));

        WebhookRequest request = new WebhookRequest(
                "ZUL0001", null, null, EventType.PARKED, -23.561684, -46.655981
        );

        assertDoesNotThrow(() -> webhookService.processEvent(request));

        verify(spotRepository, times(1)).save(argThat(s -> s.getOccupied().equals(true)));

        verify(vehicleEntryRepository, times(1)).save(argThat(entry ->
                entry.getSpotId().equals(1) &&
                        entry.getSector().equals("A") &&
                        entry.getStatus() == VehicleEntryStatus.PARKED
        ));
    }

    // -------------------------
    // handleExit — calculatePrice
    // -------------------------

    @Test
    void handleExit_shouldReturnZeroPrice_whenParkedLessThan30Minutes() {
        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0001"))
                .thenReturn(Optional.of(activeEntry));

        when(spotRepository.findById(1)).thenReturn(Optional.of(spot));
        when(sectorRepository.findBySector("A")).thenReturn(Optional.of(sectorA));

        WebhookRequest request = new WebhookRequest(
                "ZUL0001", null, "2025-01-01T10:20:00.000Z", EventType.EXIT, null, null
        );

        webhookService.processEvent(request);

        verify(vehicleEntryRepository, times(1)).save(argThat(entry ->
                entry.getPrice() != null && entry.getPrice().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    void handleExit_shouldCalculateCorrectPrice_whenParked2Hours() {
        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0001"))
                .thenReturn(Optional.of(activeEntry));

        when(spotRepository.findById(1)).thenReturn(Optional.of(spot));
        when(sectorRepository.findBySector("A")).thenReturn(Optional.of(sectorA));

        WebhookRequest request = new WebhookRequest(
                "ZUL0001", null, "2025-01-01T12:00:00.000Z", EventType.EXIT, null, null
        );

        webhookService.processEvent(request);

        verify(vehicleEntryRepository, times(1)).save(argThat(entry ->
                entry.getPrice() != null &&
                        entry.getPrice().compareTo(new BigDecimal("72.90")) == 0
        ));
    }

    @Test
    void handleExit_shouldThrowException_whenDurationLimitExceeded() {
        Sector sectorB = Sector.builder()
                .id(2L)
                .sector("B")
                .basePrice(4.1)
                .maxCapacity(20)
                .openHour("08:00")
                .closeHour("23:59")
                .durationLimitMinutes(60)
                .isFull(false)
                .build();

        VehicleEntry entryB = VehicleEntry.builder()
                .id(2L)
                .licensePlate("ZUL0004")
                .entryTime(LocalDateTime.of(2025, 1, 1, 10, 0))
                .spotId(1)
                .sector("B")
                .occupancyRateAtEntry(30.0)
                .status(VehicleEntryStatus.PARKED)
                .currency("BRL")
                .build();

        when(vehicleEntryRepository.findByLicensePlateAndExitTimeIsNull("ZUL0004"))
                .thenReturn(Optional.of(entryB));

        when(spotRepository.findById(1)).thenReturn(Optional.of(spot));
        when(sectorRepository.findBySector("B")).thenReturn(Optional.of(sectorB));

        WebhookRequest request = new WebhookRequest(
                "ZUL0004", null, "2025-01-01T11:30:00.000Z", EventType.EXIT, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> webhookService.processEvent(request));

        assertTrue(ex.getMessage().contains("duration limit"));
    }
}