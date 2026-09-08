package com.example.nawibackend.compliance.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpeFormulaEvaluatorTest {

    private final MpeFormulaEvaluator evaluator = new MpeFormulaEvaluator();

    @Test
    void evaluatesDecimalMultiplierInScaleIntervals() {
        assertTrue(new BigDecimal("0.50").compareTo(evaluator.evaluate("0.5e", new BigDecimal("1.00"))) == 0);
        assertTrue(new BigDecimal("2.00").compareTo(evaluator.evaluate("1.0e", new BigDecimal("2.00"))) == 0);
    }

    @Test
    void rejectsUnsupportedFormulaSyntax() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate("0.5%", new BigDecimal("1.00"))
        );

        assertEquals("Unsupported mpe formula: 0.5% — expected format like '0.5e' or '1.0e'", exception.getMessage());
    }
}
