package com.zendev.Estapar.repository;

import com.zendev.Estapar.model.VehicleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleEntryRepository extends JpaRepository<VehicleEntry, Long> {
    Optional<VehicleEntry> findByLicensePlateAndExitTimeIsNull(String licensePlate);
}