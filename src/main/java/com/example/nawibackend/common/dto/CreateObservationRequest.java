package com.example.nawibackend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateObservationRequest {
    private BigDecimal load;
    private BigDecimal indication;
    private BigDecimal additionalLoad;
    private boolean isDownDirection;
}
