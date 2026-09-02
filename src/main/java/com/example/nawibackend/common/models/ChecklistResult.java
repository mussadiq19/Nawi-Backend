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
public class ChecklistResult {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;
    @ManyToOne
    @JoinColumn(name = "checklist_item_id", nullable = false)
    private ChecklistItem item;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceVerdict verdict;
    private String remarks;
}
