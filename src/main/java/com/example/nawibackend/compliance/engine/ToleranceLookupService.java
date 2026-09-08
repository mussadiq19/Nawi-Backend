package com.example.nawibackend.compliance.engine;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.ToleranceRule;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ToleranceContext;
import com.example.nawibackend.compliance.repository.ToleranceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToleranceLookupService {
    private final ToleranceRuleRepository toleranceRuleRepository;
    private final MpeFormulaEvaluator mpeFormulaEvaluator;
    private final ToleranceRuleConfigurationValidator configurationValidator;

    public BigDecimal findMpe(Instrument instrument, TestType testType, BigDecimal load) {
        return findMpe(instrument, testType, ToleranceContext.INITIAL_VERIFICATION, load);
    }

    public BigDecimal findMpe(Instrument instrument, TestType testType, ToleranceContext context, BigDecimal load) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument is required to resolve an MPE.");
        }
        if (instrument.getPrimaryScale() == null || instrument.getPrimaryScale().getE() == null) {
            throw new IllegalArgumentException("Instrument verification scale interval (e) is required.");
        }
        if (load == null) {
            throw new IllegalArgumentException("Load is required to resolve an MPE.");
        }
        if (context == null) {
            throw new IllegalArgumentException("Tolerance context is required to resolve an MPE.");
        }

        BigDecimal e = instrument.getPrimaryScale().getE();
        BigDecimal loadMagnitude = load.abs();

        List<ToleranceRule> candidateRules = toleranceRuleRepository
                .findByOimlEditionAndContextAndAccuracyClassAndTestTypeOrderByLoadRangeMinAsc(
                        "R76-1:2006", context, instrument.getAccuracyClass(), testType);
        configurationValidator.validate(candidateRules);

        List<ToleranceRule> matchedRules = candidateRules.stream()
                .filter(rule -> includesLoad(rule, loadMagnitude, e))
                .toList();
        if (matchedRules.size() != 1) {
            throw new IllegalStateException("No unique tolerance rule found for R76-1:2006, accuracy class "
                    + instrument.getAccuracyClass() + ", " + testType + ", " + context
                    + ", load=" + loadMagnitude + ", e=" + e + ".");
        }

        return mpeFormulaEvaluator.evaluate(matchedRules.getFirst().getMpeFormula(), e);
    }

    private boolean includesLoad(ToleranceRule rule, BigDecimal load, BigDecimal scaleInterval) {
        BigDecimal lowerBound = rule.getLoadRangeMin() == null
                ? BigDecimal.ZERO
                : rule.getLoadRangeMin().multiply(scaleInterval);
        BigDecimal upperBound = rule.getLoadRangeMax() == null
                ? null
                : rule.getLoadRangeMax().multiply(scaleInterval);

        boolean lowerCheck = rule.isLowerBoundInclusive()
                ? load.compareTo(lowerBound) >= 0 : load.compareTo(lowerBound) > 0;
        boolean upperCheck = upperBound == null || (rule.isUpperBoundInclusive()
                ? load.compareTo(upperBound) <= 0 : load.compareTo(upperBound) < 0);
        return lowerCheck && upperCheck;
    }
}
