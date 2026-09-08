-- Seed the initial-verification tolerance rules for weighing performance as defined in
-- OIML R 76-1, Table 6. The values are represented in units of e, not hardcoded Java
-- constants. The Java service resolves the correct rule from the instrument's class, test
-- type, and applied load before evaluating the formula.

INSERT INTO tolerance_rule (id, oiml_edition, accuracy_class, test_type, load_range_min, load_range_max, mpe_formula)
VALUES
    (1, 'R76-1:2006', 'I', 'WEIGHING_PERFORMANCE', 0, 50000, '0.5e'),
    (2, 'R76-1:2006', 'I', 'WEIGHING_PERFORMANCE', 50000, 200000, '1.0e'),
    (3, 'R76-1:2006', 'I', 'WEIGHING_PERFORMANCE', 200000, NULL, '1.5e'),
    (4, 'R76-1:2006', 'II', 'WEIGHING_PERFORMANCE', 0, 5000, '0.5e'),
    (5, 'R76-1:2006', 'II', 'WEIGHING_PERFORMANCE', 5000, 20000, '1.0e'),
    (6, 'R76-1:2006', 'II', 'WEIGHING_PERFORMANCE', 20000, 100000, '1.5e'),
    (7, 'R76-1:2006', 'III', 'WEIGHING_PERFORMANCE', 0, 500, '0.5e'),
    (8, 'R76-1:2006', 'III', 'WEIGHING_PERFORMANCE', 500, 2000, '1.0e'),
    (9, 'R76-1:2006', 'III', 'WEIGHING_PERFORMANCE', 2000, 10000, '1.5e'),
    (10, 'R76-1:2006', 'IIII', 'WEIGHING_PERFORMANCE', 0, 50, '0.5e'),
    (11, 'R76-1:2006', 'IIII', 'WEIGHING_PERFORMANCE', 50, 200, '1.0e'),
    (12, 'R76-1:2006', 'IIII', 'WEIGHING_PERFORMANCE', 200, 1000, '1.5e');
