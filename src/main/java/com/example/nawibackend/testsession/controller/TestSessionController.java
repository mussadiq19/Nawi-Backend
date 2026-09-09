package com.example.nawibackend.testsession.controller;

import com.example.nawibackend.common.dto.CreateObservationRequest;
import com.example.nawibackend.common.dto.CreateTestSessionRequest;
import com.example.nawibackend.common.dto.EvaluateResponse;
import com.example.nawibackend.common.dto.ObservationResponse;
import com.example.nawibackend.common.dto.TestSessionResponse;
import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.TestSession;
import com.example.nawibackend.common.models.WeighingPerformanceObservation;
import com.example.nawibackend.common.models.enums.ComplianceVerdict;
import com.example.nawibackend.instrument.repository.InstrumentRepository;
import com.example.nawibackend.observation.repository.WeighingPerformanceObservationRepository;
import com.example.nawibackend.observation.service.WeighingPerformanceCalculationService;
import com.example.nawibackend.testsession.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-sessions")
@RequiredArgsConstructor
public class TestSessionController {

    private final TestSessionRepository testSessionRepository;
    private final InstrumentRepository instrumentRepository;
    private final WeighingPerformanceObservationRepository observationRepository;
    private final WeighingPerformanceCalculationService calculationService;

    /**
     * POST /test-sessions — create a new test session.
     */
    @PostMapping
    public ResponseEntity<TestSessionResponse> createSession(@RequestBody CreateTestSessionRequest request) {
        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instrument not found with id: " + request.getInstrumentId()));

        TestSession session = TestSession.builder()
                .instrument(instrument)
                .date(request.getDate())
                .observer(request.getObserver())
                .verificationScaleInterval(request.getVerificationScaleInterval())
                .resolutionDuringTest(request.getResolutionDuringTest())
                .testType(request.getTestType())
                .verdict(ComplianceVerdict.NOT_APPLICABLE)
                .remarks(request.getRemarks())
                .build();

        TestSession saved = testSessionRepository.save(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSessionResponse(saved, List.of()));
    }

    /**
     * GET /test-sessions/{sessionId} — return persisted session data without recomputing.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<TestSessionResponse> getSession(@PathVariable Long sessionId) {
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Test session not found with id: " + sessionId));

        List<WeighingPerformanceObservation> observations =
                observationRepository.findBySessionId(sessionId);

        return ResponseEntity.ok(toSessionResponse(session, observations));
    }

    /**
     * POST /test-sessions/{sessionId}/observations — add one observation row to a session.
     */
    @PostMapping("/{sessionId}/observations")
    public ResponseEntity<ObservationResponse> addObservation(
            @PathVariable Long sessionId,
            @RequestBody CreateObservationRequest request) {

        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Test session not found with id: " + sessionId));

        WeighingPerformanceObservation obs = WeighingPerformanceObservation.builder()
                .session(session)
                .load(request.getLoad())
                .indication(request.getIndication())
                .additionalLoad(request.getAdditionalLoad())
                .isDownDirection(request.isDownDirection())
                .build();

        WeighingPerformanceObservation saved = observationRepository.save(obs);
        return ResponseEntity.status(HttpStatus.CREATED).body(toObservationResponse(saved));
    }

    /**
     * POST /test-sessions/{sessionId}/evaluate — trigger calculation and return full result.
     */
    @PostMapping("/{sessionId}/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(@PathVariable Long sessionId) {
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Test session not found with id: " + sessionId));

        calculationService.calculateAndEvaluate(session);

        List<WeighingPerformanceObservation> observations =
                observationRepository.findBySessionId(sessionId);

        List<ObservationResponse> observationResponses = observations.stream()
                .map(this::toObservationResponse)
                .toList();

        return ResponseEntity.ok(EvaluateResponse.builder()
                .sessionId(session.getId())
                .verdict(session.getVerdict())
                .observations(observationResponses)
                .build());
    }

    // --- mapping helpers ---

    private TestSessionResponse toSessionResponse(TestSession session,
                                                  List<WeighingPerformanceObservation> observations) {
        List<ObservationResponse> obsResponses = observations.stream()
                .map(this::toObservationResponse)
                .toList();

        return TestSessionResponse.builder()
                .id(session.getId())
                .instrumentId(session.getInstrument().getId())
                .date(session.getDate())
                .observer(session.getObserver())
                .testType(session.getTestType())
                .verificationScaleInterval(session.getVerificationScaleInterval())
                .resolutionDuringTest(session.getResolutionDuringTest())
                .verdict(session.getVerdict())
                .remarks(session.getRemarks())
                .observations(obsResponses)
                .build();
    }

    private ObservationResponse toObservationResponse(WeighingPerformanceObservation obs) {
        boolean passed = obs.getCorrectedError() != null && obs.getMpe() != null
                && obs.getCorrectedError().abs().compareTo(obs.getMpe().abs()) <= 0;

        return ObservationResponse.builder()
                .id(obs.getId())
                .load(obs.getLoad())
                .indication(obs.getIndication())
                .additionalLoad(obs.getAdditionalLoad())
                .isDownDirection(obs.isDownDirection())
                .error(obs.getError())
                .correctedError(obs.getCorrectedError())
                .mpe(obs.getMpe())
                .passed(passed)
                .build();
    }
}
