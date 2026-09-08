package com.example.nawibackend.common.models.embadable;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ScaleIntervalSet {
    private BigDecimal e;
    private BigDecimal max;
    private BigDecimal d;
    private Integer n;
}
