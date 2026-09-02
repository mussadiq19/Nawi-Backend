package com.example.nawibackend.common.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class WeighingPerformanceObservation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    private BigDecimal load;
    private BigDecimal indication;
    private BigDecimal additionalLoad;
    private boolean isDownDirection;
    private BigDecimal error;
    private BigDecimal correctedError;
    private BigDecimal mpe;
}
