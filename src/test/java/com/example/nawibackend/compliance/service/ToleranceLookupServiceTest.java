package com.example.nawibackend.compliance.service;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.ToleranceRule;
import com.example.nawibackend.common.models.embadable.ScaleIntervalSet;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ToleranceContext;
import com.example.nawibackend.compliance.engine.MpeFormulaEvaluator;
import com.example.nawibackend.compliance.engine.ToleranceLookupService;
import com.example.nawibackend.compliance.repository.ToleranceRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToleranceLookupServiceTest {

    @Test
    void picksTheExactRuleAtTheLowerBoundary() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        ToleranceLookupService service = new ToleranceLookupService(repository, new MpeFormulaEvaluator());

        Instrument instrument = validInstrument();

        when(repository.findByAccuracyClassAndTestType(AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of(
                        rule(0L, 50000L, "0.5e"),
                        rule(50000L, 200000L, "1.0e"),
                        rule(200000L, null, "1.5e")
                ));

        assertTrue(new BigDecimal("0.50").compareTo(
                service.findMpe(instrument, TestType.WEIGHING_PERFORMANCE, new BigDecimal("50000"))) == 0);
    }

    @Test
    void movesToTheNextRuleWhenTheLoadIsJustAboveBoundary() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        ToleranceLookupService service = new ToleranceLookupService(repository, new MpeFormulaEvaluator());

        Instrument instrument = validInstrument();

        when(repository.findByAccuracyClassAndTestType(AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of(
                        rule(0L, 50000L, "0.5e"),
                        rule(50000L, 200000L, "1.0e"),
                        rule(200000L, null, "1.5e")
                ));

        assertTrue(new BigDecimal("1.00").compareTo(
                service.findMpe(instrument, TestType.WEIGHING_PERFORMANCE, new BigDecimal("50000.01"))) == 0);
    }

    @Test
    void rejectsInvalidLookupInputsBeforeQueryingRules() {
        ToleranceLookupService service = new ToleranceLookupService(
                mock(ToleranceRuleRepository.class), new MpeFormulaEvaluator());

        assertThrows(IllegalArgumentException.class,
                () -> service.findMpe(null, TestType.WEIGHING_PERFORMANCE, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> service.findMpe(validInstrument(), null, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> service.findMpe(validInstrument(), TestType.WEIGHING_PERFORMANCE, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.findMpe(validInstrument(), TestType.WEIGHING_PERFORMANCE, new BigDecimal("-0.001")));
    }

    @Test
    void rejectsLoadWhenNoRuleBandMatches() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        ToleranceLookupService service = new ToleranceLookupService(repository, new MpeFormulaEvaluator());

        when(repository.findByAccuracyClassAndTestType(AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.findMpe(
                validInstrument(), TestType.WEIGHING_PERFORMANCE, BigDecimal.ZERO));
    }

    @Test
    void picksFirstMatchingRuleDeterministicallyWhenBandsOverlap() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        ToleranceLookupService service = new ToleranceLookupService(repository, new MpeFormulaEvaluator());

        Instrument instrument = validInstrument();

        when(repository.findByAccuracyClassAndTestType(AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of(
                        rule(0L, 50000L, "0.5e"),
                        rule(40000L, 60000L, "1.0e")
                ));

        // Load 45000 falls in BOTH bands; the first matching rule (0.5e) wins.
        assertTrue(new BigDecimal("0.50").compareTo(
                service.findMpe(instrument, TestType.WEIGHING_PERFORMANCE, new BigDecimal("45000"))) == 0);
    }

    @Test
    void preservesHighPrecisionLoadForRuleSelection() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        ToleranceLookupService service = new ToleranceLookupService(repository, new MpeFormulaEvaluator());
        Instrument instrument = validInstrument(new BigDecimal("0.001"), 50000, 100);

        when(repository.findByAccuracyClassAndTestType(AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of(
                        rule(0L, 50000L, "0.5e"),
                        rule(50000L, 200000L, "1.0e"),
                        rule(200000L, null, "1.5e")
                ));

        assertTrue(new BigDecimal("0.0010").compareTo(
                service.findMpe(instrument, TestType.WEIGHING_PERFORMANCE, new BigDecimal("50.0000000001"))) == 0);
    }

    private ToleranceRule rule(long min, Long max, String formula) {
        ToleranceRule rule = new ToleranceRule();
        rule.setOimlEdition("R76-1:2006");
        rule.setSourceReference("3.5.1 / Table 6");
        rule.setContext(ToleranceContext.INITIAL_VERIFICATION);
        rule.setAccuracyClass(AccuracyClass.I);
        rule.setTestType(TestType.WEIGHING_PERFORMANCE);
        rule.setLoadRangeMin(new BigDecimal(min));
        rule.setLoadRangeMax(max == null ? null : new BigDecimal(max));
        rule.setLowerBoundInclusive(min == 0);
        rule.setUpperBoundInclusive(true);
        rule.setMpeFormula(formula);
        return rule;
    }

    private Instrument validInstrument() {
        return validInstrument(BigDecimal.ONE, 50000, 100);
    }

    private Instrument validInstrument(BigDecimal e, int n, int minMultiplier) {
        ScaleIntervalSet scale = new ScaleIntervalSet();
        scale.setE(e);
        scale.setMax(e.multiply(BigDecimal.valueOf(n)));
        scale.setN(n);
        Instrument instrument = new Instrument();
        instrument.setAccuracyClass(AccuracyClass.I);
        instrument.setMin(e.multiply(BigDecimal.valueOf(minMultiplier)));
        instrument.setPrimaryScale(scale);
        instrument.setAuxiliaryIndicatingDevice(false);
        return instrument;
    }
}