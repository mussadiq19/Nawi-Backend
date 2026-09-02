package com.example.nawibackend.common.models;

import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import jakarta.persistence.*;
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
    @Column(nullable = false)
    private String oimlEdition;          // "R76-1:2006" — supports future revisions
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccuracyClass accuracyClass;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestType testType;
    private BigDecimal loadRangeMin, loadRangeMax;   // in units of e
    @Column(nullable = false)
    private String mpeFormula;           // e.g. "0.5e" / "1.0e" / "1.5e" per R76-1 §3.6
}
