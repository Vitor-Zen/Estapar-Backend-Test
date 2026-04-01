package com.zendev.Estapar.repository;

import com.zendev.Estapar.enums.VehicleEntryStatus;
import com.zendev.Estapar.model.VehicleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehicleEntryRepository extends JpaRepository<VehicleEntry, Long> {
    Optional<VehicleEntry> findByLicensePlateAndExitTimeIsNull(String licensePlate);
    List<VehicleEntry> findBySectorAndExitTimeBetweenAndStatusIs(
            String sector,
            LocalDateTime start,
            LocalDateTime end,
            VehicleEntryStatus status
    );
}