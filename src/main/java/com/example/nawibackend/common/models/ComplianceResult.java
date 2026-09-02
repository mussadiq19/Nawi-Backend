package com.example.nawibackend.common.models;

import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Entity
public class ComplianceResult {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceVerdict verdict;
    private String computedMpe;
    private String actualDeviation;
    private LocalDateTime evaluatedAt;
}
