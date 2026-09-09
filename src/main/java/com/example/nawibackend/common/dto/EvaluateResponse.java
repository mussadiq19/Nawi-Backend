package com.example.nawibackend.common.dto;

import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluateResponse {
    private Long sessionId;
    private ComplianceVerdict verdict;
    private List<ObservationResponse> observations;
}
