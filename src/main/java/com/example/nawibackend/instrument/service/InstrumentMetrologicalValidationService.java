package com.example.nawibackend.instrument.service;

import com.example.nawibackend.common.models.Instrument;
import com.example.nawibackend.common.models.embadable.ScaleIntervalSet;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Source-bounded validation before an instrument is used for an MPE lookup. */
@Service
public class InstrumentMetrologicalValidationService {

    public void validateForMpeLookup(Instrument instrument) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument is required for metrological validation.");
        }
        if (instrument.getAccuracyClass() == null) {
            throw new IllegalArgumentException("Instrument accuracy class is required.");
        }
        if (instrument.getAuxiliaryIndicatingDevice() == null) {
            throw new IllegalArgumentException("Whether an auxiliary indicating device is fitted must be declared.");
        }
        if (instrument.getAdditionalRanges() != null && !instrument.getAdditionalRanges().isEmpty()) {
            throw new IllegalArgumentException("Multiple or multi-interval ranges cannot be validated: each additional range lacks required Min and range-type information.");
        }

        ScaleIntervalSet scale = instrument.getPrimaryScale();
        if (scale == null) {
            throw new IllegalArgumentException("Primary scale characteristics are required.");
        }
        requirePositive(scale.getE(), "Verification scale interval (e)");
        requirePositive(scale.getMax(), "Maximum capacity (Max)");
        requirePositive(instrument.getMin(), "Minimum capacity (Min)");
        if (instrument.getMin().compareTo(scale.getMax()) > 0) {
            throw new IllegalArgumentException("Minimum capacity (Min) cannot exceed maximum capacity (Max).");
        }
        if (scale.getD() != null) {
            requirePositive(scale.getD(), "Actual scale interval (d)");
        }
        validateNumberOfVerificationIntervals(instrument.getAccuracyClass(), scale);
        validateMinimumCapacity(instrument.getAccuracyClass(), instrument.getMin(), scale.getE());
        validateAuxiliaryIndication(instrument, scale);
    }

    private void validateNumberOfVerificationIntervals(AccuracyClass accuracyClass, ScaleIntervalSet scale) {
        if (scale.getN() == null || scale.getN() <= 0) {
            throw new IllegalArgumentException("Number of verification scale intervals (n) must be positive.");
        }
        BigDecimal[] quotientAndRemainder = scale.getMax().divideAndRemainder(scale.getE());
        if (quotientAndRemainder[1].compareTo(BigDecimal.ZERO) != 0
                || quotientAndRemainder[0].compareTo(BigDecimal.valueOf(scale.getN())) != 0) {
            throw new IllegalArgumentException("Number of verification scale intervals must equal Max/e exactly.");
        }

        int n = scale.getN();
        int minimum = switch (accuracyClass) {
            case I -> 50_000;
            case II, III, IIII -> 100;
        };
        Integer maximum = switch (accuracyClass) {
            case I -> null;
            case II -> 100_000;
            case III -> 10_000;
            case IIII -> 1_000;
        };
        if (n < minimum) {
            throw new IllegalArgumentException("Accuracy class " + accuracyClass + " instrument has n=" + n
                    + ", below the applicable Table 3 minimum of " + minimum + ".");
        }
        if (maximum != null && n > maximum) {
            throw new IllegalArgumentException("Accuracy class " + accuracyClass + " instrument has n=" + n
                    + ", exceeding the applicable Table 3 maximum of " + maximum + ".");
        }
    }

    private void validateMinimumCapacity(AccuracyClass accuracyClass, BigDecimal min, BigDecimal e) {
        int multiplier = switch (accuracyClass) {
            case I -> 100;
            case II, III -> 20;
            case IIII -> 10;
        };
        BigDecimal requiredMin = e.multiply(BigDecimal.valueOf(multiplier));
        if (min.compareTo(requiredMin) < 0) {
            throw new IllegalArgumentException("Accuracy class " + accuracyClass + " instrument has Min=" + min
                    + ", below the applicable Table 3 minimum of " + multiplier + "e.");
        }
    }

    private void validateAuxiliaryIndication(Instrument instrument, ScaleIntervalSet scale) {
        if (!instrument.getAuxiliaryIndicatingDevice()) {
            return;
        }
        if (instrument.getAccuracyClass() != AccuracyClass.I && instrument.getAccuracyClass() != AccuracyClass.II) {
            throw new IllegalArgumentException("Auxiliary indicating devices are only supported for accuracy class I or II.");
        }
        if (scale.getD() == null) {
            throw new IllegalArgumentException("Actual scale interval (d) is required when an auxiliary indicating device is fitted.");
        }
        if (scale.getD().compareTo(scale.getE()) >= 0
                || scale.getE().compareTo(scale.getD().multiply(BigDecimal.TEN)) > 0) {
            throw new IllegalArgumentException("For an auxiliary indicating device, R76-1 3.4.2 requires d < e <= 10d.");
        }
    }

    private void requirePositive(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
