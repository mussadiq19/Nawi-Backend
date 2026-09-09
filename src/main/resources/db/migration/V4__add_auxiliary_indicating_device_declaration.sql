-- The nullable declaration records whether the R76-1 3.4 auxiliary-indicating
-- device rules apply. Existing records remain unknown and are rejected by the
-- metrological validator until explicitly classified.
ALTER TABLE instrument
    ADD COLUMN auxiliary_indicating_device boolean;
