package com.example.nawibackend.compliance.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MpeFormulaEvaluator {

    // Matches a decimal multiplier followed by a lowercase 'e', with no other content,
    // e.g. "0.5e", "1.0e", "1.5e", "0.75e", "2.5e". The decimal point is required so
    // the formula stays canonical ("1e" is rejected, not silently treated as "1.0e").
    // Uppercase "1.5E" is intentionally rejected since OIML R76 writes the scale
    // interval as lowercase 'e'.
    private static final Pattern DECIMAL_E_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+)e");

    public BigDecimal evaluate(String mpeFormula, BigDecimal scaleIntervalE) {
        if (scaleIntervalE == null || scaleIntervalE.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Verification scale interval (e) must be positive to evaluate an MPE formula.");
        }

        if (mpeFormula == null || mpeFormula.isBlank()) {
            throw new IllegalArgumentException("MPE formula is required.");
        }

        Matcher matcher = DECIMAL_E_PATTERN.matcher(mpeFormula);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Unsupported MPE formula: " + mpeFormula
                            + " — expected a '<decimal>e' formula such as \"0.5e\", \"1.0e\", or \"1.5e\". "
                            + "Percentage and fixed-value formulas are not supported."
            );
        }

        BigDecimal multiplier = new BigDecimal(matcher.group(1));
        return multiplier.multiply(scaleIntervalE);
    }
}