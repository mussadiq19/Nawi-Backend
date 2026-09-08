package com.example.nawibackend.testsession.repository;

import com.example.nawibackend.common.models.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
}
