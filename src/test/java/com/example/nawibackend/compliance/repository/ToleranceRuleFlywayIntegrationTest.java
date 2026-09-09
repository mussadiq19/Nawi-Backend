package com.example.nawibackend.compliance.repository;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.ToleranceRule;
import com.example.nawibackend.common.models.embadable.ScaleIntervalSet;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ToleranceContext;
import com.example.nawibackend.compliance.engine.ToleranceLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses the Flyway V1–V3 path; Hibernate only validates the resulting schema. */
@SpringBootTest
class ToleranceRuleFlywayIntegrationTest {

    @Autowired
    private ToleranceRuleRepository repository;

    @Autowired
    private ToleranceLookupService lookupService;

    @Test
    void flywaySeedsAllInitialVerificationTableSixRulesWithTraceability() {
        List<ToleranceRule> rules = repository
                .findByAccuracyClassAndTestType(AccuracyClass.III, TestType.WEIGHING_PERFORMANCE);

        assertEquals(3, rules.size());
        assertEquals(0, rules.getFirst().getLoadRangeMin().compareTo(new BigDecimal("0")));
        assertTrue(rules.getFirst().isLowerBoundInclusive());
        assertEquals(0, rules.get(1).getLoadRangeMin().compareTo(new BigDecimal("500")));
        assertTrue(!rules.get(1).isLowerBoundInclusive());
        assertEquals("3.5.1 / Table 6", rules.get(2).getSourceReference());
        assertEquals(ToleranceContext.INITIAL_VERIFICATION, rules.get(2).getContext());
        assertEquals("1.5e", rules.get(2).getMpeFormula());
    }

    @Test
    void migratedRowsMatchEveryTableSixBand() {
        assertBands(AccuracyClass.I, new String[][]{{"0", "50000", "0.5e"}, {"50000", "200000", "1.0e"}, {"200000", null, "1.5e"}});
        assertBands(AccuracyClass.II, new String[][]{{"0", "5000", "0.5e"}, {"5000", "20000", "1.0e"}, {"20000", "100000", "1.5e"}});
        assertBands(AccuracyClass.III, new String[][]{{"0", "500", "0.5e"}, {"500", "2000", "1.0e"}, {"2000", "10000", "1.5e"}});
        assertBands(AccuracyClass.IIII, new String[][]{{"0", "50", "0.5e"}, {"50", "200", "1.0e"}, {"200", "1000", "1.5e"}});
    }

    @Test
    void selectsClassThreeExactAndJustAboveBoundariesFromMigratedRows() {
        Instrument instrument = instrument(AccuracyClass.III, "1");

        assertMpe(instrument, "500", "0.5");
        assertMpe(instrument, "500.001", "1.0");
        assertMpe(instrument, "2000", "1.0");
        assertMpe(instrument, "2000.001", "1.5");
    }

    @Test
    void selectsOpenEndedClassOneFinalBandFromMigratedRows() {
        assertMpe(instrument(AccuracyClass.I, "1"), "200000.001", "1.5");
    }

    @Test
    void selectsTableSixTransitionBandsForEveryAccuracyClass() {
        assertMpe(instrument(AccuracyClass.I, "1"), "50000", "0.5");
        assertMpe(instrument(AccuracyClass.I, "1"), "50000.001", "1.0");
        assertMpe(instrument(AccuracyClass.II, "1"), "5000", "0.5");
        assertMpe(instrument(AccuracyClass.II, "1"), "5000.001", "1.0");
        assertMpe(instrument(AccuracyClass.III, "1"), "500", "0.5");
        assertMpe(instrument(AccuracyClass.III, "1"), "500.001", "1.0");
        assertMpe(instrument(AccuracyClass.IIII, "1"), "50", "0.5");
        assertMpe(instrument(AccuracyClass.IIII, "1"), "50.001", "1.0");
    }

    @Test
    @Transactional
    void overlappingBandResolvesToFirstMatchingRuleInsteadOfThrowing() {
        ToleranceRule overlapping = new ToleranceRule();
        overlapping.setOimlEdition("R76-1:2006");
        overlapping.setSourceReference("test overlap");
        overlapping.setContext(ToleranceContext.INITIAL_VERIFICATION);
        overlapping.setAccuracyClass(AccuracyClass.III);
        overlapping.setTestType(TestType.WEIGHING_PERFORMANCE);
        overlapping.setLoadRangeMin(new BigDecimal("400"));
        overlapping.setLoadRangeMax(new BigDecimal("600"));
        overlapping.setLowerBoundInclusive(true);
        overlapping.setUpperBoundInclusive(true);
        overlapping.setMpeFormula("0.5e");
        repository.saveAndFlush(overlapping);

        // The simplified lookup iterates bands in repository order and picks the first
        // match. The seeded (0, 500] 0.5e band precedes the overlapping (400, 600] row,
        // so load 500 still resolves to 0.5e — deterministically, no exception.
        assertEquals(0, lookupService.findMpe(instrument(AccuracyClass.III, "1"),
                TestType.WEIGHING_PERFORMANCE, new BigDecimal("500")).compareTo(new BigDecimal("0.5")));
    }

    private void assertMpe(Instrument instrument, String load, String expected) {
        BigDecimal result = lookupService.findMpe(instrument, TestType.WEIGHING_PERFORMANCE,
                new BigDecimal(load));
        assertEquals(0, result.compareTo(new BigDecimal(expected)));
    }

    private Instrument instrument(AccuracyClass accuracyClass, String e) {
        ScaleIntervalSet scale = new ScaleIntervalSet();
        scale.setE(new BigDecimal(e));
        int n = switch (accuracyClass) {
            case I -> 50_000;
            case II -> 5_000;
            case III -> 500;
            case IIII -> 100;
        };
        int minMultiplier = switch (accuracyClass) {
            case I -> 100;
            case II, III -> 20;
            case IIII -> 10;
        };
        scale.setMax(scale.getE().multiply(BigDecimal.valueOf(n)));
        scale.setN(n);
        Instrument instrument = new Instrument();
        instrument.setAccuracyClass(accuracyClass);
        instrument.setMin(scale.getE().multiply(BigDecimal.valueOf(minMultiplier)));
        instrument.setPrimaryScale(scale);
        instrument.setAuxiliaryIndicatingDevice(false);
        return instrument;
    }

    private void assertBands(AccuracyClass accuracyClass, String[][] expectedBands) {
        List<ToleranceRule> rules = repository
                .findByAccuracyClassAndTestType(accuracyClass, TestType.WEIGHING_PERFORMANCE);
        assertEquals(expectedBands.length, rules.size());
        for (int index = 0; index < expectedBands.length; index++) {
            ToleranceRule rule = rules.get(index);
            assertEquals(0, rule.getLoadRangeMin().compareTo(new BigDecimal(expectedBands[index][0])));
            if (expectedBands[index][1] == null) {
                assertNull(rule.getLoadRangeMax());
            } else {
                assertEquals(0, rule.getLoadRangeMax().compareTo(new BigDecimal(expectedBands[index][1])));
            }
            assertEquals(expectedBands[index][2], rule.getMpeFormula());
            assertEquals(index == 0, rule.isLowerBoundInclusive());
            assertTrue(rule.isUpperBoundInclusive());
            assertEquals("R76-1:2006", rule.getOimlEdition());
            assertEquals("3.5.1 / Table 6", rule.getSourceReference());
            assertEquals(ToleranceContext.INITIAL_VERIFICATION, rule.getContext());
        }
    }
}
