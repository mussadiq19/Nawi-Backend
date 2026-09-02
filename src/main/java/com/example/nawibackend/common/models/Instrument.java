package com.example.nawibackend.common.models;

import com.example.nawibackend.common.models.embadable.ScaleIntervalSet;
import com.example.nawibackend.common.models.embadable.ZeroTareDeviceConfig;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.IndicationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Instrument {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String applicationNo;

    @Column(nullable = false)
    private String typeDesignation;

    private String manufacturer;
    private String applicant;
    private String instrumentCategory;

    private boolean isModule;
    private BigDecimal errorFractionPi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccuracyClass accuracyClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IndicationType indicationType;

    private BigDecimal min;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "e_primary"))
    })
    private ScaleIntervalSet primaryScale;

    @ElementCollection
    @CollectionTable(name = "instrument_scale_ranges")
    private List<ScaleIntervalSet> additionalRanges;

    private BigDecimal tPlus;
    private BigDecimal tMinus;

    private BigDecimal uNom, uMin, uMax;
    private BigDecimal frequencyHz;
    private BigDecimal batteryUNom;

    @Embedded
    private ZeroTareDeviceConfig deviceConfig;

    private BigDecimal initialZeroSettingRangePercent;
    private BigDecimal temperatureRangeC;

    private String printerStatus;

    private String identificationNo;
    private String softwareVersion;
    private String connectedEquipment;
    private String interfaces;

    private String loadCellManufacturer;
    private String loadCellType;
    private BigDecimal loadCellCapacity;
    private String loadCellNumber;
    private String loadCellClassificationSymbol;

    private LocalDate evaluationPeriodStart, evaluationPeriodEnd;
    private LocalDate dateOfReport;
    private String remarks;
}




