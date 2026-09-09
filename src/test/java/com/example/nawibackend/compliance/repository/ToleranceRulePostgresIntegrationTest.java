package com.example.nawibackend.compliance.repository;

import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Opt-in real PostgreSQL migration test. Enable with NAWI_POSTGRES_IT=true and
 * supply NAWI_POSTGRES_URL, NAWI_POSTGRES_USERNAME, and NAWI_POSTGRES_PASSWORD.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "NAWI_POSTGRES_IT", matches = "true")
class ToleranceRulePostgresIntegrationTest {

    @Autowired
    private ToleranceRuleRepository repository;

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("NAWI_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("NAWI_POSTGRES_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("NAWI_POSTGRES_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void migratesAndReadsClassThreeTableSixRulesOnPostgresql() {
        assertEquals(3, repository
                .findByAccuracyClassAndTestType(AccuracyClass.III, TestType.WEIGHING_PERFORMANCE)
                .size());
    }
}
