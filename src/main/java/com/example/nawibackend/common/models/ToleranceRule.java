package com.example.nawibackend.common.models;

import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ToleranceContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ToleranceRule {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank
    @Column(nullable = false)
    private String oimlEdition;          // "R76-1:2006" — supports future revisions
    @NotBlank
    @Column(nullable = false)
    private String sourceReference;      // e.g. "3.5.1 / Table 6"
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ToleranceContext context;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccuracyClass accuracyClass;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestType testType;
    @NotNull
    @Column(nullable = false)
    private BigDecimal loadRangeMin; // in units of e
    private BigDecimal loadRangeMax; // null means no upper bound, in units of e
    @Column(nullable = false)
    private boolean lowerBoundInclusive;
    @Column(nullable = false)
    private boolean upperBoundInclusive;
    @NotBlank
    @Column(nullable = false)
    private String mpeFormula;           // e.g. "0.5e" / "1.0e" / "1.5e" per R76-1 §3.6
}
