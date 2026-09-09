package com.example.nawibackend.instrument.controller;

import com.example.nawibackend.common.dto.TestSessionResponse;
import com.example.nawibackend.common.models.TestSession;
import com.example.nawibackend.instrument.repository.InstrumentRepository;
import com.example.nawibackend.testsession.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final TestSessionRepository testSessionRepository;
    private final InstrumentRepository instrumentRepository;

    /**
     * GET /instruments/{instrumentId}/test-sessions — list sessions for an instrument.
     */
    @GetMapping("/{instrumentId}/test-sessions")
    public ResponseEntity<List<TestSessionResponse>> listSessionsForInstrument(
            @PathVariable Long instrumentId) {

        if (!instrumentRepository.existsById(instrumentId)) {
            throw new IllegalArgumentException(
                    "Instrument not found with id: " + instrumentId);
        }

        List<TestSession> sessions = testSessionRepository.findAll().stream()
                .filter(s -> s.getInstrument().getId().equals(instrumentId))
                .toList();

        List<TestSessionResponse> responses = sessions.stream()
                .map(this::toSessionResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    private TestSessionResponse toSessionResponse(TestSession session) {
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
                .observations(List.of())  // list endpoint returns sessions without observation details
                .build();
    }
}
