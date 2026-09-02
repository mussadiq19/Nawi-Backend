package com.example.nawibackend.common.models;

import com.example.nawibackend.common.models.embadable.EnvironmentalConditions;
import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ZeroTrackingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class TestSession {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    private LocalDate date;
    private String observer;
    private BigDecimal verificationScaleInterval;
    private BigDecimal resolutionDuringTest;

    @Embedded
    private EnvironmentalConditions envConditions;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private ZeroTrackingStatus zeroTrackingStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestType testType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceVerdict verdict;

    private String remarks;
}
