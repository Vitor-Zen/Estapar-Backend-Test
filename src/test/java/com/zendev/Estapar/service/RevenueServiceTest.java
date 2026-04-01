package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.RevenueRequest;
import com.zendev.Estapar.dto.RevenueResponse;
import com.zendev.Estapar.enums.VehicleEntryStatus;
import com.zendev.Estapar.model.VehicleEntry;
import com.zendev.Estapar.repository.VehicleEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private VehicleEntryRepository vehicleEntryRepository;

    @InjectMocks
    private RevenueService revenueService;

    // -------------------------
    // getRevenue
    // -------------------------

    @Test
    void getRevenue_shouldReturnSumOfExitedVehicles() {
        RevenueRequest request = new RevenueRequest(LocalDate.of(2025, 1, 1), "A");

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 2, 0, 0);

        VehicleEntry entry1 = VehicleEntry.builder()
                .licensePlate("ZUL0001")
                .sector("A")
                .price(new BigDecimal("72.90"))
                .status(VehicleEntryStatus.EXITED)
                .exitTime(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();

        VehicleEntry entry2 = VehicleEntry.builder()
                .licensePlate("ZUL0002")
                .sector("A")
                .price(new BigDecimal("40.50"))
                .status(VehicleEntryStatus.EXITED)
                .exitTime(LocalDateTime.of(2025, 1, 1, 14, 0))
                .build();

        when(vehicleEntryRepository.findBySectorAndExitTimeBetweenAndStatusIs(
                "A", start, end, VehicleEntryStatus.EXITED
        )).thenReturn(List.of(entry1, entry2));

        RevenueResponse response = revenueService.getRevenue(request);

        assertEquals(new BigDecimal("113.40"), response.amount());
        assertEquals("BRL", response.currency());
        assertNotNull(response.timestamp());
    }

    @Test
    void getRevenue_shouldReturnZero_whenNoVehiclesExitedOnDate() {
        RevenueRequest request = new RevenueRequest(LocalDate.of(2025, 1, 1), "A");

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 2, 0, 0);

        when(vehicleEntryRepository.findBySectorAndExitTimeBetweenAndStatusIs(
                "A", start, end, VehicleEntryStatus.EXITED
        )).thenReturn(List.of());

        RevenueResponse response = revenueService.getRevenue(request);

        assertEquals(BigDecimal.ZERO, response.amount());
        assertEquals("BRL", response.currency());
        assertNotNull(response.timestamp());
    }

    @Test
    void getRevenue_shouldIgnoreVehiclesStillParked() {
        RevenueRequest request = new RevenueRequest(LocalDate.of(2025, 1, 1), "A");

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 2, 0, 0);

        VehicleEntry exitedEntry = VehicleEntry.builder()
                .licensePlate("ZUL0001")
                .sector("A")
                .price(new BigDecimal("72.90"))
                .status(VehicleEntryStatus.EXITED)
                .exitTime(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();

        when(vehicleEntryRepository.findBySectorAndExitTimeBetweenAndStatusIs(
                "A", start, end, VehicleEntryStatus.EXITED
        )).thenReturn(List.of(exitedEntry));

        RevenueResponse response = revenueService.getRevenue(request);

        assertEquals(new BigDecimal("72.90"), response.amount());
    }

    @Test
    void getRevenue_shouldIgnoreEntriesWithNullPrice() {
        RevenueRequest request = new RevenueRequest(LocalDate.of(2025, 1, 1), "A");

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 2, 0, 0);

        VehicleEntry nullPriceEntry = VehicleEntry.builder()
                .licensePlate("ZUL0003")
                .sector("A")
                .price(null)
                .status(VehicleEntryStatus.EXITED)
                .exitTime(LocalDateTime.of(2025, 1, 1, 10, 30))
                .build();

        when(vehicleEntryRepository.findBySectorAndExitTimeBetweenAndStatusIs(
                "A", start, end, VehicleEntryStatus.EXITED
        )).thenReturn(List.of(nullPriceEntry));

        assertDoesNotThrow(() -> revenueService.getRevenue(request));

        RevenueResponse response = revenueService.getRevenue(request);

        assertEquals(BigDecimal.ZERO, response.amount());
    }
}
