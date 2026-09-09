package com.example.nawibackend.instrument.service;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.embadable.ScaleIntervalSet;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentMetrologicalValidationServiceTest {

    private final InstrumentMetrologicalValidationService validator = new InstrumentMetrologicalValidationService();

    @Test
    void acceptsValidBoundaryConfigurationsForAllClasses() {
        assertDoesNotThrow(() -> validator.validateForMpeLookup(instrument(AccuracyClass.I, 50_000, 100, false, null)));
        assertDoesNotThrow(() -> validator.validateForMpeLookup(instrument(AccuracyClass.II, 5_000, 20, false, null)));
        assertDoesNotThrow(() -> validator.validateForMpeLookup(instrument(AccuracyClass.III, 500, 20, false, null)));
        assertDoesNotThrow(() -> validator.validateForMpeLookup(instrument(AccuracyClass.IIII, 100, 10, false, null)));
    }

    @Test
    void rejectsMissingOrNonPositiveCoreValues() {
        Instrument missingClass = instrument(AccuracyClass.III, 500, 20, false, null);
        missingClass.setAccuracyClass(null);
        assertInvalid(missingClass);
        Instrument missingMax = instrument(AccuracyClass.III, 500, 20, false, null);
        missingMax.getPrimaryScale().setMax(null);
        assertInvalid(missingMax);
        Instrument missingMin = instrument(AccuracyClass.III, 500, 20, false, null);
        missingMin.setMin(null);
        assertInvalid(missingMin);
        Instrument invalidE = instrument(AccuracyClass.III, 500, 20, false, null);
        invalidE.getPrimaryScale().setE(BigDecimal.ZERO);
        assertInvalid(invalidE);
        Instrument invalidD = instrument(AccuracyClass.III, 500, 20, false, null);
        invalidD.getPrimaryScale().setD(BigDecimal.ZERO);
        assertInvalid(invalidD);
    }

    @Test
    void rejectsNThatIsNotExactlyMaxDividedByEOrViolatesClassBounds() {
        Instrument mismatchedN = instrument(AccuracyClass.III, 500, 20, false, null);
        mismatchedN.getPrimaryScale().setN(499);
        assertInvalid(mismatchedN);
        assertInvalid(instrument(AccuracyClass.I, 49_999, 100, false, null));
        assertInvalid(instrument(AccuracyClass.II, 100_001, 20, false, null));
        assertInvalid(instrument(AccuracyClass.III, 10_001, 20, false, null));
        assertInvalid(instrument(AccuracyClass.IIII, 1_001, 10, false, null));
    }

    @Test
    void rejectsClassSpecificMinimumCapacityViolation() {
        assertInvalid(instrument(AccuracyClass.I, 50_000, 99, false, null));
        assertInvalid(instrument(AccuracyClass.II, 5_000, 19, false, null));
        assertInvalid(instrument(AccuracyClass.III, 500, 19, false, null));
        assertInvalid(instrument(AccuracyClass.IIII, 100, 9, false, null));
    }

    @Test
    void appliesDToERelationshipOnlyWhenAuxiliaryDeviceIsDeclared() {
        assertDoesNotThrow(() -> validator.validateForMpeLookup(
                instrument(AccuracyClass.II, 5_000, 20, true, new BigDecimal("0.1"))));
        assertInvalid(instrument(AccuracyClass.II, 5_000, 20, true, BigDecimal.ONE));
        assertInvalid(instrument(AccuracyClass.II, 5_000, 20, true, new BigDecimal("0.09")));
        assertInvalid(instrument(AccuracyClass.III, 500, 20, true, new BigDecimal("0.1")));
    }

    @Test
    void rejectsUnknownAuxiliaryStatusAndAdditionalRanges() {
        Instrument unknownAuxiliary = instrument(AccuracyClass.III, 500, 20, false, null);
        unknownAuxiliary.setAuxiliaryIndicatingDevice(null);
        assertInvalid(unknownAuxiliary);
        Instrument multiRange = instrument(AccuracyClass.III, 500, 20, false, null);
        multiRange.setAdditionalRanges(List.of(new ScaleIntervalSet()));
        assertInvalid(multiRange);
    }

    private Instrument instrument(AccuracyClass accuracyClass, int n, int min, boolean auxiliary, BigDecimal d) {
        ScaleIntervalSet scale = new ScaleIntervalSet();
        scale.setE(BigDecimal.ONE);
        scale.setMax(BigDecimal.valueOf(n));
        scale.setN(n);
        scale.setD(d);
        Instrument instrument = new Instrument();
        instrument.setAccuracyClass(accuracyClass);
        instrument.setMin(BigDecimal.valueOf(min));
        instrument.setPrimaryScale(scale);
        instrument.setAuxiliaryIndicatingDevice(auxiliary);
        return instrument;
    }

    private void assertInvalid(Instrument instrument) {
        assertThrows(IllegalArgumentException.class, () -> validator.validateForMpeLookup(instrument));
    }
}
