package com.example.nawibackend.compliance.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpeFormulaEvaluatorTest {

    private final MpeFormulaEvaluator evaluator = new MpeFormulaEvaluator();

    @Test
    void evaluatesTableSixMultipliersWithPrecision() {
        assertTrue(new BigDecimal("0.50").compareTo(evaluator.evaluate("0.5e", new BigDecimal("1.00"))) == 0);
        assertTrue(new BigDecimal("1.0").compareTo(evaluator.evaluate("1.0e", new BigDecimal("1"))) == 0);
        assertTrue(new BigDecimal("0.0015").compareTo(evaluator.evaluate("1.5e", new BigDecimal("0.001"))) == 0);
    }

    @Test
    void evaluatesAnyDecimalMultiplierInTheDecimalEPattern() {
        // The "<decimal>e" pattern accepts multipliers beyond the current Table 6 values,
        // keeping tolerance formulas DB-driven and extensible for future OIML revisions.
        assertTrue(new BigDecimal("0.75").compareTo(evaluator.evaluate("0.75e", new BigDecimal("1"))) == 0);
        assertTrue(new BigDecimal("2.5").compareTo(evaluator.evaluate("2.5e", new BigDecimal("1"))) == 0);
        assertTrue(new BigDecimal("0.0025").compareTo(evaluator.evaluate("2.50e", new BigDecimal("0.001"))) == 0);
    }

    @Test
    void rejectsNullBlankAndUnsupportedFormulaForms() {
        for (String formula : new String[]{null, "", "   ", "0.5%", "1e", "1.5E", "1.0e + 0.5e", "e", "1.0 e", "-0.5e"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> evaluator.evaluate(formula, new BigDecimal("1")));
        }
    }

    @Test
    void rejectsNullZeroAndNegativeVerificationScaleIntervals() {
        for (BigDecimal e : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("-0.001")}) {
            assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("0.5e", e));
        }
    }
}
