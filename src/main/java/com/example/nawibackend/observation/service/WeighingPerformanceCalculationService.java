package com.example.nawibackend.observation.service;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.TestSession;
import com.example.nawibackend.common.models.WeighingPerformanceObservation;
import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.compliance.engine.ToleranceLookupService;
import com.example.nawibackend.observation.repository.WeighingPerformanceObservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeighingPerformanceCalculationService {

    private final WeighingPerformanceObservationRepository observationRepository;
    private final ToleranceLookupService toleranceLookupService;

    @Transactional
    public void calculateAndEvaluate(TestSession session) {
        List<WeighingPerformanceObservation> observations =
                observationRepository.findBySessionId(session.getId());

        Instrument instrument = session.getInstrument();
        BigDecimal e = instrument.getPrimaryScale().getE();

        for (WeighingPerformanceObservation obs : observations) {
            BigDecimal error = calculateError(obs.getIndication(), e,
                    obs.getAdditionalLoad(), obs.getLoad());
            obs.setError(error);
        }
        BigDecimal e0 = findZeroLoadError(observations)
                .orElseThrow(() -> new IllegalStateException(
                        "No zero-load observation found in session " + session.getId() +
                                " — cannot compute corrected error without a baseline"
                ));

        for (WeighingPerformanceObservation obs : observations) {
            obs.setCorrectedError(obs.getError().subtract(e0));
        }
        boolean allPassed = true;
        for (WeighingPerformanceObservation obs : observations) {
            BigDecimal mpe = toleranceLookupService.findMpe(
                    instrument, TestType.WEIGHING_PERFORMANCE, obs.getLoad()
            );
            obs.setMpe(mpe);

            boolean rowPassed = obs.getCorrectedError().abs()
                    .compareTo(mpe.abs()) <= 0;
            if (!rowPassed) {
                allPassed = false;
            }
        }

        observationRepository.saveAll(observations);

        // Step 6: session-level verdict — one failing row fails the whole session
        session.setVerdict(allPassed ? ComplianceVerdict.PASSED : ComplianceVerdict.FAILED);
    }

    private BigDecimal calculateError(BigDecimal indication, BigDecimal e,
                                      BigDecimal additionalLoad, BigDecimal load) {
        return indication
                .add(e.divide(BigDecimal.valueOf(2)))
                .subtract(additionalLoad)
                .subtract(load);
    }

    private Optional<BigDecimal> findZeroLoadError(List<WeighingPerformanceObservation> observations) {
        return observations.stream()
                .filter(obs -> obs.getLoad().compareTo(BigDecimal.ZERO) == 0)
                .findFirst()
                .map(WeighingPerformanceObservation::getError);
    }
}
