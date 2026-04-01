package com.zendev.Estapar.service;

import com.zendev.Estapar.dto.RevenueRequest;
import com.zendev.Estapar.dto.RevenueResponse;
import com.zendev.Estapar.enums.VehicleEntryStatus;
import com.zendev.Estapar.model.VehicleEntry;
import com.zendev.Estapar.repository.VehicleEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RevenueService {

    private final VehicleEntryRepository vehicleEntryRepository;

    public RevenueService(VehicleEntryRepository vehicleEntryRepository) {
        this.vehicleEntryRepository = vehicleEntryRepository;
    }

    // Calculates total revenue for a given sector and date
    // Filters only EXITED vehicles within the full day range
    // Sums all prices, ignoring null values
    public RevenueResponse getRevenue(RevenueRequest request){
        LocalDateTime start = request.date().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<VehicleEntry> entries = vehicleEntryRepository
                .findBySectorAndExitTimeBetweenAndStatusIs(
                        request.sector(),
                        start,
                        end,
                        VehicleEntryStatus.EXITED
                );

        BigDecimal total = entries.stream()
                .map(VehicleEntry::getPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RevenueResponse(
                total,
                "BRL",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.000'Z'"))
        );
    }
}
