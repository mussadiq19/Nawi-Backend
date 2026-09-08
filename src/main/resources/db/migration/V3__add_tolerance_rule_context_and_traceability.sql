-- Make Table 6 rule applicability explicit. R 76-1:2006 3.5.1/Table 6 defines
-- initial-verification bands; 3.5.2 defines a separate service context.
ALTER TABLE tolerance_rule
    ADD COLUMN source_reference varchar(255) NOT NULL DEFAULT '3.5.1 / Table 6';
ALTER TABLE tolerance_rule
    ADD COLUMN context varchar(30) NOT NULL DEFAULT 'INITIAL_VERIFICATION';
ALTER TABLE tolerance_rule
    ADD COLUMN lower_bound_inclusive boolean NOT NULL DEFAULT true;
ALTER TABLE tolerance_rule
    ADD COLUMN upper_bound_inclusive boolean NOT NULL DEFAULT true;

ALTER TABLE tolerance_rule
    ADD CONSTRAINT ck_tolerance_rule_context
        CHECK (context IN ('INITIAL_VERIFICATION', 'SERVICE'));

-- Table 6 successor bands are lower-exclusive. A NULL upper bound remains the
-- explicit representation of an unbounded upper interval.
UPDATE tolerance_rule
SET source_reference = '3.5.1 / Table 6',
    context = 'INITIAL_VERIFICATION',
    lower_bound_inclusive = CASE WHEN load_range_min = 0 THEN true ELSE false END,
    upper_bound_inclusive = true;

-- V2 inserted explicit identifiers 1–12; advance Hibernate's sequence so a
-- subsequently managed rule cannot collide with a seeded Table 6 row.
ALTER SEQUENCE tolerance_rule_seq RESTART WITH 13;
