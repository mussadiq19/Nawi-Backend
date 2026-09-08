package com.example.nawibackend.compliance.engine;

import com.example.nawibackend.common.models.ToleranceRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Validates the candidate rules before a regulatory lookup uses them. */
@Component
public class ToleranceRuleConfigurationValidator {

    public void validate(List<ToleranceRule> rules) {
        for (ToleranceRule rule : rules) {
            validateRule(rule);
        }
        for (int left = 0; left < rules.size(); left++) {
            for (int right = left + 1; right < rules.size(); right++) {
                if (overlaps(rules.get(left), rules.get(right))) {
                    throw new IllegalStateException("Overlapping tolerance-rule configuration detected.");
                }
            }
        }
    }

    private void validateRule(ToleranceRule rule) {
        if (rule.getOimlEdition() == null || rule.getOimlEdition().isBlank()
                || rule.getSourceReference() == null || rule.getSourceReference().isBlank()
                || rule.getContext() == null || rule.getAccuracyClass() == null
                || rule.getTestType() == null || rule.getMpeFormula() == null || rule.getMpeFormula().isBlank()
                || rule.getLoadRangeMin() == null) {
            throw new IllegalStateException("Incomplete tolerance-rule configuration detected.");
        }
        if (rule.getLoadRangeMin().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Tolerance-rule lower boundary cannot be negative.");
        }
        if (rule.getLoadRangeMax() != null
                && rule.getLoadRangeMax().compareTo(rule.getLoadRangeMin()) < 0) {
            throw new IllegalStateException("Tolerance-rule upper boundary cannot be below its lower boundary.");
        }
        if (rule.getLoadRangeMax() == null && !rule.isUpperBoundInclusive()) {
            throw new IllegalStateException("An unbounded tolerance rule must use the explicit unbounded upper-bound representation.");
        }
    }

    private boolean overlaps(ToleranceRule first, ToleranceRule second) {
        return startsBeforeEnd(first, second) && startsBeforeEnd(second, first);
    }

    private boolean startsBeforeEnd(ToleranceRule start, ToleranceRule end) {
        if (end.getLoadRangeMax() == null) {
            return true;
        }
        int comparison = start.getLoadRangeMin().compareTo(end.getLoadRangeMax());
        return comparison < 0 || (comparison == 0 && start.isLowerBoundInclusive() && end.isUpperBoundInclusive());
    }
}
