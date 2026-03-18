package com.time.timecalc.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.time.timecalc.model.CodeSnapshot;

public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, UUID> {
    // Найти последний анализ для проекта
    Optional<CodeSnapshot> findFirstByProjectIdOrderByAnalyzedAtDesc(UUID projectId);
}
