package com.example.nawibackend.common.dto;

import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import com.example.nawibackend.common.models.enums.TestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestSessionResponse {
    private Long id;
    private Long instrumentId;
    private LocalDate date;
    private String observer;
    private TestType testType;
    private BigDecimal verificationScaleInterval;
    private BigDecimal resolutionDuringTest;
    private ComplianceVerdict verdict;
    private String remarks;
    private List<ObservationResponse> observations;
}
