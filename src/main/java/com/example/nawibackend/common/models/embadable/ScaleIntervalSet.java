package com.example.nawibackend.common.models.embadable;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class ScaleIntervalSet {
    private BigDecimal e;
    private BigDecimal max;
    private BigDecimal d;
    private Integer n;
}
