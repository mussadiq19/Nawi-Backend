package com.example.nawibackend.common.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChecklistItem {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private String requirementCode;
    private String testingProcedureRef;
    private String description;
    @Column(nullable = false)
    private String category;
}
