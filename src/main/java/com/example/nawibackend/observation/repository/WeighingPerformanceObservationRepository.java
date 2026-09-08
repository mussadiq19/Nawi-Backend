package com.example.nawibackend.observation.repository;

import com.example.nawibackend.common.models.WeighingPerformanceObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface WeighingPerformanceObservationRepository extends JpaRepository <WeighingPerformanceObservation, Long>{
    List<WeighingPerformanceObservation> findBySessionId(Long sessionId);
}
