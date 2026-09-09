package com.example.nawibackend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ObservationResponse {
    private Long id;
    private BigDecimal load;
    private BigDecimal indication;
    private BigDecimal additionalLoad;
    private boolean isDownDirection;
    private BigDecimal error;
    private BigDecimal correctedError;
    private BigDecimal mpe;
    private boolean passed;
}
