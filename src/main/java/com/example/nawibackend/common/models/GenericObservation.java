package com.example.nawibackend.common.models;

import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class GenericObservation {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    @Column(columnDefinition = "json")
    private String observationData;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComplianceVerdict verdict;
    private String remarks;
}
