package com.example.nawibackend.common.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class TemperatureEffectObservation {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    private LocalDate date;
    private LocalTime time;
    private BigDecimal temperature;
    private BigDecimal zeroIndication, additionalLoad;
    private BigDecimal p;
    private BigDecimal deltaP, deltaTemp;
}
