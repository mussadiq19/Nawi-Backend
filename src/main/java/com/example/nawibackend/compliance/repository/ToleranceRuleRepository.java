package com.example.nawibackend.compliance.repository;

import com.example.nawibackend.common.models.ToleranceRule;
import com.example.nawibackend.common.models.enums.AccuracyClass;
import com.example.nawibackend.common.models.enums.TestType;
import com.example.nawibackend.common.models.enums.ToleranceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ToleranceRuleRepository extends JpaRepository<ToleranceRule, Long> {
    List<ToleranceRule> findByOimlEditionAndContextAndAccuracyClassAndTestTypeOrderByLoadRangeMinAsc(
            String oimlEdition, ToleranceContext context, AccuracyClass accuracyClass, TestType testType
    );
}
