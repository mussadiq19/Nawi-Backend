-- =============================================================================
-- V1__init_schema.sql
-- Initial schema for NawiBackend (OIML R76-1 NAWI type-evaluation records).
--
-- Column/table names follow Spring Boot's default
-- CamelCaseToUnderscoresNamingStrategy, and id sequences follow Hibernate 6's
-- default for @GeneratedValue(AUTO) on a Long: one sequence per table named
-- <table>_seq with allocationSize 50.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Sequences (Hibernate AUTO -> SequenceStyleGenerator, allocationSize = 50)
-- -----------------------------------------------------------------------------
CREATE SEQUENCE instrument_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE test_session_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE weighing_performance_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE eccentricity_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tilting_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE discrimination_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE temperature_effect_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE repeatability_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE generic_observation_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE checklist_item_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE checklist_result_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tolerance_rule_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE compliance_result_seq START WITH 1 INCREMENT BY 50;

-- -----------------------------------------------------------------------------
-- instrument
-- -----------------------------------------------------------------------------
CREATE TABLE instrument
(
    id                                 bigint         NOT NULL,

    application_no                     varchar(255)   NOT NULL,
    type_designation                   varchar(255)   NOT NULL,
    manufacturer                       varchar(255),
    applicant                          varchar(255),
    instrument_category                varchar(255),

    is_module                          boolean        NOT NULL,
    error_fraction_pi                  numeric(38, 2),

    accuracy_class                     varchar(10)    NOT NULL,
    indication_type                    varchar(30)    NOT NULL,

    min                                numeric(38, 2),

    -- @Embedded ScaleIntervalSet primaryScale
    e                                  numeric(38, 2),
    max                                numeric(38, 2),
    d                                  numeric(38, 2),
    n                                  integer,

    t_plus                             numeric(38, 2),
    t_minus                            numeric(38, 2),

    u_nom                              numeric(38, 2),
    u_min                              numeric(38, 2),
    u_max                              numeric(38, 2),
    frequency_hz                       numeric(38, 2),
    batteryunom                        numeric(38, 2),

    -- @Embedded ZeroTareDeviceConfig deviceConfig
    non_automatic_zero_setting         boolean        NOT NULL,
    semi_automatic_zero_setting        boolean        NOT NULL,
    automatic_zero_setting             boolean        NOT NULL,
    initial_zero_setting               boolean        NOT NULL,
    zero_tracking                      boolean        NOT NULL,
    tare_balancing                     boolean        NOT NULL,
    tare_weighing                      boolean        NOT NULL,
    combined_zero_tare_device          boolean        NOT NULL,
    preset_tare_device                 boolean        NOT NULL,
    subtractive_tare                   boolean        NOT NULL,
    additive_tare                      boolean        NOT NULL,

    initial_zero_setting_range_percent numeric(38, 2),
    temperature_rangec                 numeric(38, 2),

    printer_status                     varchar(255),

    identification_no                  varchar(255),
    software_version                   varchar(255),
    connected_equipment                varchar(255),
    interfaces                         varchar(255),

    load_cell_manufacturer             varchar(255),
    load_cell_type                     varchar(255),
    load_cell_capacity                 numeric(38, 2),
    load_cell_number                   varchar(255),
    load_cell_classification_symbol    varchar(255),

    evaluation_period_start            date,
    evaluation_period_end              date,
    date_of_report                     date,
    remarks                            varchar(255),

    CONSTRAINT pk_instrument PRIMARY KEY (id)
);

-- @ElementCollection List<ScaleIntervalSet> additionalRanges
CREATE TABLE instrument_scale_ranges
(
    instrument_id bigint NOT NULL,
    e             numeric(38, 2),
    max           numeric(38, 2),
    d             numeric(38, 2),
    n             integer
);

-- -----------------------------------------------------------------------------
-- test_session
-- -----------------------------------------------------------------------------
CREATE TABLE test_session
(
    id                          bigint         NOT NULL,
    instrument_id               bigint         NOT NULL,

    date                        date,
    observer                    varchar(255),
    verification_scale_interval numeric(38, 2),
    resolution_during_test      numeric(38, 2),

    -- @Embedded EnvironmentalConditions envConditions
    temp_start                  numeric(38, 2),
    temp_max                    numeric(38, 2),
    temp_end                    numeric(38, 2),
    rel_humidity_start          numeric(38, 2),
    rel_humidity_max            numeric(38, 2),
    rel_humidity_end            numeric(38, 2),
    time_start                  time(6),
    time_max                    time(6),
    time_end                    time(6),
    bar_pres_start              numeric(38, 2),
    bar_pres_max                numeric(38, 2),
    bar_pres_end                numeric(38, 2),

    zero_tracking_status        varchar(25),
    test_type                   varchar(30)    NOT NULL,
    verdict                     varchar(20)    NOT NULL,

    remarks                     varchar(255),

    CONSTRAINT pk_test_session PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- Observation tables
-- -----------------------------------------------------------------------------
CREATE TABLE weighing_performance_observation
(
    id                 bigint  NOT NULL,
    session_id         bigint  NOT NULL,

    load               numeric(38, 2),
    indication         numeric(38, 2),
    additional_load    numeric(38, 2),
    is_down_direction  boolean NOT NULL,
    error              numeric(38, 2),
    corrected_error    numeric(38, 2),
    mpe                numeric(38, 2),

    CONSTRAINT pk_weighing_performance_observation PRIMARY KEY (id)
);

CREATE TABLE eccentricity_observation
(
    id              bigint NOT NULL,
    session_id      bigint NOT NULL,

    location        integer,
    section         varchar(255),
    direction       varchar(255),

    load            numeric(38, 2),
    indication      numeric(38, 2),
    additional_load numeric(38, 2),
    error           numeric(38, 2),
    corrected_error numeric(38, 2),
    mpe             numeric(38, 2),

    CONSTRAINT pk_eccentricity_observation PRIMARY KEY (id)
);

CREATE TABLE tilting_observation
(
    id                             bigint NOT NULL,
    session_id                     bigint NOT NULL,

    "position"                     integer,
    load_level                     numeric(38, 2),

    indication                     numeric(38, 2),
    additional_load                numeric(38, 2),
    error                          numeric(38, 2),
    corrected_error                numeric(38, 2),
    mpe                            numeric(38, 2),

    max_difference_from_reference  numeric(38, 2),

    CONSTRAINT pk_tilting_observation PRIMARY KEY (id)
);

CREATE TABLE discrimination_observation
(
    id                    bigint NOT NULL,
    session_id            bigint NOT NULL,

    load                  numeric(38, 2),
    indication1           numeric(38, 2),
    extra_load            numeric(38, 2),
    indication2           numeric(38, 2),
    indication_difference numeric(38, 2),

    CONSTRAINT pk_discrimination_observation PRIMARY KEY (id)
);

CREATE TABLE temperature_effect_observation
(
    id              bigint NOT NULL,
    session_id      bigint NOT NULL,

    date            date,
    time            time(6),
    temperature     numeric(38, 2),
    zero_indication numeric(38, 2),
    additional_load numeric(38, 2),
    p               numeric(38, 2),
    deltap          numeric(38, 2),
    delta_temp      numeric(38, 2),

    CONSTRAINT pk_temperature_effect_observation PRIMARY KEY (id)
);

CREATE TABLE repeatability_observation
(
    id              bigint NOT NULL,
    session_id      bigint NOT NULL,

    weighing_number integer,
    indication      numeric(38, 2),
    additional_load numeric(38, 2),
    error           numeric(38, 2),

    CONSTRAINT pk_repeatability_observation PRIMARY KEY (id)
);

CREATE TABLE generic_observation
(
    id               bigint NOT NULL,
    session_id       bigint NOT NULL,

    observation_data json,
    verdict          varchar(20),
    remarks          varchar(255),

    CONSTRAINT pk_generic_observation PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- Checklist
-- -----------------------------------------------------------------------------
CREATE TABLE checklist_item
(
    id                    bigint       NOT NULL,
    requirement_code      varchar(255) NOT NULL,
    testing_procedure_ref varchar(255),
    description           varchar(255),
    category              varchar(255) NOT NULL,

    CONSTRAINT pk_checklist_item PRIMARY KEY (id)
);

CREATE TABLE checklist_result
(
    id                bigint      NOT NULL,
    session_id        bigint      NOT NULL,
    checklist_item_id bigint      NOT NULL,
    verdict           varchar(20) NOT NULL,
    remarks           varchar(255),

    CONSTRAINT pk_checklist_result PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- Tolerance rules / compliance
-- -----------------------------------------------------------------------------
CREATE TABLE tolerance_rule
(
    id              bigint       NOT NULL,
    oiml_edition    varchar(255) NOT NULL,
    accuracy_class  varchar(10)  NOT NULL,
    test_type       varchar(30)  NOT NULL,
    load_range_min  numeric(38, 2),
    load_range_max  numeric(38, 2),
    mpe_formula     varchar(255) NOT NULL,

    CONSTRAINT pk_tolerance_rule PRIMARY KEY (id)
);

CREATE TABLE compliance_result
(
    id               bigint      NOT NULL,
    session_id       bigint      NOT NULL,
    verdict          varchar(20) NOT NULL,
    computed_mpe     varchar(255),
    actual_deviation varchar(255),
    evaluated_at     timestamp(6),

    CONSTRAINT pk_compliance_result PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- user  (GenerationType.IDENTITY -- no sequence)
-- -----------------------------------------------------------------------------
CREATE TABLE "user"
(
    id            bigint GENERATED BY DEFAULT AS IDENTITY,
    username      varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    role          varchar(15)  NOT NULL,

    CONSTRAINT pk_user PRIMARY KEY (id)
);

-- =============================================================================
-- Foreign keys
-- =============================================================================
ALTER TABLE instrument_scale_ranges
    ADD CONSTRAINT fk_instrument_scale_ranges_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument (id);

ALTER TABLE test_session
    ADD CONSTRAINT fk_test_session_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument (id);

ALTER TABLE weighing_performance_observation
    ADD CONSTRAINT fk_weighing_performance_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE eccentricity_observation
    ADD CONSTRAINT fk_eccentricity_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE tilting_observation
    ADD CONSTRAINT fk_tilting_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE discrimination_observation
    ADD CONSTRAINT fk_discrimination_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE temperature_effect_observation
    ADD CONSTRAINT fk_temperature_effect_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE repeatability_observation
    ADD CONSTRAINT fk_repeatability_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE generic_observation
    ADD CONSTRAINT fk_generic_observation_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE checklist_result
    ADD CONSTRAINT fk_checklist_result_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

ALTER TABLE checklist_result
    ADD CONSTRAINT fk_checklist_result_checklist_item
        FOREIGN KEY (checklist_item_id) REFERENCES checklist_item (id);

ALTER TABLE compliance_result
    ADD CONSTRAINT fk_compliance_result_session
        FOREIGN KEY (session_id) REFERENCES test_session (id);

-- =============================================================================
-- Enum CHECK constraints
--
-- Deliberately NOT constrained: test_type. That enum is expected to grow as
-- further R76-1 test procedures are modelled; a hardcoded list here would force
-- a migration for every new value.
-- =============================================================================
ALTER TABLE instrument
    ADD CONSTRAINT ck_instrument_accuracy_class
        CHECK (accuracy_class IN ('I', 'II', 'III', 'IIII'));

ALTER TABLE instrument
    ADD CONSTRAINT ck_instrument_indication_type
        CHECK (indication_type IN ('SELF_INDICATING', 'SEMI_SELF_INDICATING', 'NON_SELF_INDICATING'));

ALTER TABLE test_session
    ADD CONSTRAINT ck_test_session_verdict
        CHECK (verdict IN ('PASSED', 'FAILED', 'NOT_APPLICABLE'));

ALTER TABLE test_session
    ADD CONSTRAINT ck_test_session_zero_tracking_status
        CHECK (zero_tracking_status IN ('NON_EXISTENT', 'NOT_IN_OPERATION', 'OUT_OF_WORKING_RANGE', 'IN_OPERATION'));

ALTER TABLE generic_observation
    ADD CONSTRAINT ck_generic_observation_verdict
        CHECK (verdict IN ('PASSED', 'FAILED', 'NOT_APPLICABLE'));

ALTER TABLE checklist_result
    ADD CONSTRAINT ck_checklist_result_verdict
        CHECK (verdict IN ('PASSED', 'FAILED', 'NOT_APPLICABLE'));

ALTER TABLE compliance_result
    ADD CONSTRAINT ck_compliance_result_verdict
        CHECK (verdict IN ('PASSED', 'FAILED', 'NOT_APPLICABLE'));

ALTER TABLE "user"
    ADD CONSTRAINT ck_user_role
        CHECK (role IN ('ADMIN', 'LAB_OFFICER', 'TECHNICIAN', 'VIEWER'));
