package com.example.nawibackend.common.models.embadable;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EnvironmentalConditions {
    private BigDecimal tempStart, tempMax, tempEnd;
    private BigDecimal relHumidityStart, relHumidityMax, relHumidityEnd;
    private LocalTime timeStart, timeMax, timeEnd;
    private BigDecimal barPresStart, barPresMax, barPresEnd;
}
