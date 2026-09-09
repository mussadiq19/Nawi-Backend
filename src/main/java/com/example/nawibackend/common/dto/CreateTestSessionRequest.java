package com.example.nawibackend.common.dto;

import com.example.nawibackend.common.models.enums.TestType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTestSessionRequest {
    private Long instrumentId;
    private LocalDate date;
    private String observer;
    private BigDecimal verificationScaleInterval;
    private BigDecimal resolutionDuringTest;
    private TestType testType;
    private String remarks;
}
