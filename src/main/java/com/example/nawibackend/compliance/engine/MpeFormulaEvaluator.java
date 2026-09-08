package com.example.nawibackend.compliance.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MpeFormulaEvaluator {
    private static final Pattern MULTIPLIER_PATTERN =
            Pattern.compile("^\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*([eE])\\s*$");

    public BigDecimal evaluate(String mpeFormula, BigDecimal scaleIntervalE) {
        if (scaleIntervalE == null) {
            throw new IllegalArgumentException("Scale interval is required to evaluate an mpe formula.");
        }

        if (mpeFormula == null || mpeFormula.isBlank()) {
            throw new IllegalArgumentException("MPE formula is required.");
        }

        Matcher matcher = MULTIPLIER_PATTERN.matcher(mpeFormula.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Unsupported mpe formula: " + mpeFormula +
                            " — expected format like '0.5e' or '1.0e'"
            );
        }

        BigDecimal multiplier = new BigDecimal(matcher.group(1));
        return multiplier.multiply(scaleIntervalE);
    }
}