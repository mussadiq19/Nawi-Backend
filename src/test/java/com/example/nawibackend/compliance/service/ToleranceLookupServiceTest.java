package com.example.nawibackend.compliance.service;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.ToleranceRule;
import com.example.nawibackend.common.models.embadable.ScaleIntervalSet;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ToleranceContext;
import com.example.nawibackend.compliance.engine.MpeFormulaEvaluator;
import com.example.nawibackend.compliance.engine.ToleranceRuleConfigurationValidator;
import com.example.nawibackend.compliance.engine.ToleranceLookupService;
import com.example.nawibackend.compliance.repository.ToleranceRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToleranceLookupServiceTest {

    @Test
    void picksTheExactRuleAtTheLowerBoundary() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        MpeFormulaEvaluator evaluator = new MpeFormulaEvaluator();
        ToleranceLookupService service = new ToleranceLookupService(repository, evaluator,
                new ToleranceRuleConfigurationValidator());

        Instrument instrument = new Instrument();
        instrument.setAccuracyClass(AccuracyClass.I);
        ScaleIntervalSet scale = new ScaleIntervalSet();
        scale.setE(new BigDecimal("1.00"));
        instrument.setPrimaryScale(scale);

        when(repository.findByOimlEditionAndContextAndAccuracyClassAndTestTypeOrderByLoadRangeMinAsc(
                "R76-1:2006", ToleranceContext.INITIAL_VERIFICATION,
                AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of(
                        rule(0L, 50000L, "0.5e"),
                        rule(50000L, 200000L, "1.0e"),
                        rule(200000L, null, "1.5e")
                ));

        assertTrue(new BigDecimal("0.50").compareTo(service.findMpe(instrument, TestType.WEIGHING_PERFORMANCE, new BigDecimal("50000"))) == 0);
    }

    @Test
    void movesToTheNextRuleWhenTheLoadIsJustAboveBoundary() {
        ToleranceRuleRepository repository = mock(ToleranceRuleRepository.class);
        MpeFormulaEvaluator evaluator = new MpeFormulaEvaluator();
        ToleranceLookupService service = new ToleranceLookupService(repository, evaluator,
                new ToleranceRuleConfigurationValidator());

        Instrument instrument = new Instrument();
        instrument.setAccuracyClass(AccuracyClass.I);
        ScaleIntervalSet scale = new ScaleIntervalSet();
        scale.setE(new BigDecimal("1.00"));
        instrument.setPrimaryScale(scale);

        when(repository.findByOimlEditionAndContextAndAccuracyClassAndTestTypeOrderByLoadRangeMinAsc(
                "R76-1:2006", ToleranceContext.INITIAL_VERIFICATION,
                AccuracyClass.I, TestType.WEIGHING_PERFORMANCE))
                .thenReturn(List.of(
                        rule(0L, 50000L, "0.5e"),
                        rule(50000L, 200000L, "1.0e"),
                        rule(200000L, null, "1.5e")
                ));

        assertTrue(new BigDecimal("1.00").compareTo(service.findMpe(instrument, TestType.WEIGHING_PERFORMANCE, new BigDecimal("50000.01"))) == 0);
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
}
