package com.example.nawibackend.compliance.engine;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.ToleranceRule;
import com.example.nawibackend.common.models.enums.TestType;
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

    /**
     * Resolves the maximum permissible error (mpe) for a given instrument, test type,
     * and load value by looking up the matching ToleranceRule row and evaluating its formula.
     *
     * @param instrument the instrument whose accuracy class and scale interval (e) are used
     * @param testType   the OIML R76 test type (e.g. WEIGHING_PERFORMANCE)
     * @param load       the test load value (in the same units as the instrument's readings)
     * @return the computed mpe as a BigDecimal
     * @throws IllegalStateException if no tolerance rule matches the given load
     */
    public BigDecimal findMpe(Instrument instrument, TestType testType, BigDecimal load) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument is required to resolve an MPE.");
        }
        if (testType == null) {
            throw new IllegalArgumentException("Test type is required to resolve an MPE.");
        }
        if (load == null) {
            throw new IllegalArgumentException("Load is required to resolve an MPE.");
        }
        if (load.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Load must not be negative when resolving an MPE.");
        }

        BigDecimal e = instrument.getPrimaryScale().getE();

        List<ToleranceRule> rules = toleranceRuleRepository
                .findByAccuracyClassAndTestType(instrument.getAccuracyClass(), testType);

        // Find the single rule whose load range contains this load.
        //
        // Half-open interval convention: min < load <= max
        // ------------------------------------------------
        // This matches how OIML R76-1's cascading load bands work in practice:
        // a load that falls exactly on a boundary belongs to the NEXT (tighter) band,
        // not the one whose upper limit it touches. For example, if band A covers
        // (0, 500e] and band B covers (500e, 2000e], then a load of exactly 500e
        // belongs to band A, and 500.01e belongs to band B. The lower bound is
        // exclusive and the upper bound is inclusive.
        //
        // To avoid floating-point/rounding errors at exact boundaries, we do NOT
        // divide load by e and compare against the rule's range (which is in units of e).
        // Instead, we multiply each rule's loadRangeMin/loadRangeMax by e to get absolute
        // boundary values, then compare the raw load against those.
        ToleranceRule matched = null;
        for (ToleranceRule rule : rules) {
            BigDecimal absoluteMin = rule.getLoadRangeMin().multiply(e);
            BigDecimal absoluteMax = rule.getLoadRangeMax() == null
                    ? null
                    : rule.getLoadRangeMax().multiply(e);

            boolean aboveMin = load.compareTo(absoluteMin) > 0;  // exclusive lower bound
            boolean atOrBelowMax = absoluteMax == null || load.compareTo(absoluteMax) <= 0;  // inclusive upper bound

            if (aboveMin && atOrBelowMax) {
                matched = rule;
                break;
            }
        }

        if (matched == null) {
            throw new IllegalStateException(
                    "No tolerance rule found for accuracy class " + instrument.getAccuracyClass()
                            + ", test type " + testType + ", load=" + load + ", e=" + e + "."
            );
        }

        return mpeFormulaEvaluator.evaluate(matched.getMpeFormula(), e);
    }
}
