package com.zendev.Estapar.repository;

import com.zendev.Estapar.model.Sector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findBySector(String sector);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sector s order by s.sector")
    List<Sector> findAllForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sector s where s.sector = :sector")
    Optional<Sector> findBySectorForUpdate(String sector);

}