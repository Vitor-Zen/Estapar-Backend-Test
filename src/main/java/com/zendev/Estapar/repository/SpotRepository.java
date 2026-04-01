package com.zendev.Estapar.repository;

import com.zendev.Estapar.model.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpotRepository extends JpaRepository<Spot, Integer> {
    List<Spot> findBySector(String sector);
    Optional<Spot> findByLatAndLng(Double lat, Double lng);
}