package com.example.nawibackend.observation.service;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.TestSession;
import com.example.nawibackend.common.models.WeighingPerformanceObservation;
import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.compliance.engine.ToleranceLookupService;
import com.example.nawibackend.observation.repository.WeighingPerformanceObservationRepository;
import com.example.nawibackend.testsession.repository.TestSessionRepository;
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
    private final TestSessionRepository testSessionRepository;

    /**
     * Computes errors, corrected errors, and mpe evaluations for every observation
     * in the session, then persists the results and sets the session-level verdict.
     *
     * <p>This method is @Transactional because it writes to two separate entity types
     * (WeighingPerformanceObservation and TestSession) in a single operation. Without
     * the transaction boundary, a failure after saving observations but before saving
     * the session verdict would leave the database in an inconsistent state —
     * observations with computed values but no corresponding session verdict. The
     * transaction ensures both writes succeed or neither does.</p>
     *
     * @param session the test session to evaluate — must already be persisted and have observations
     */
    @Transactional
    public void calculateAndEvaluate(TestSession session) {
        List<WeighingPerformanceObservation> observations =
                observationRepository.findBySessionId(session.getId());

        Instrument instrument = session.getInstrument();
        BigDecimal e = instrument.getPrimaryScale().getE();

        // Step 1: compute raw error for every observation
        // E = I + (e/2) - ΔL - L
        for (WeighingPerformanceObservation obs : observations) {
            BigDecimal error = calculateError(obs.getIndication(), e,
                    obs.getAdditionalLoad(), obs.getLoad());
            obs.setError(error);
        }

        // Step 2: find the zero-load baseline (E0)
        BigDecimal e0 = findZeroLoadError(observations)
                .orElseThrow(() -> new IllegalStateException(
                        "No zero-load observation found in session " + session.getId() +
                                " — cannot compute corrected error without a baseline"
                ));

        // Step 3: compute corrected error for every observation
        // Ec = E - E0
        for (WeighingPerformanceObservation obs : observations) {
            obs.setCorrectedError(obs.getError().subtract(e0));
        }

        // Step 4: look up mpe and determine pass/fail for every observation
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

        // Step 5: persist all updated observations
        observationRepository.saveAll(observations);

        // Step 6: set and persist session-level verdict
        // One failing observation fails the whole session.
        // This explicit save() is critical — without it, the session verdict change
        // is only in the in-memory entity and would not be flushed to the database,
        // causing callers to see a stale verdict. This was a real bug in an earlier draft.
        session.setVerdict(allPassed ? ComplianceVerdict.PASSED : ComplianceVerdict.FAILED);
        testSessionRepository.save(session);
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
