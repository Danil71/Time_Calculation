package com.time.timecalc.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.time.timecalc.model.CodeSnapshot;

public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, UUID> {
    // Найти последний анализ для проекта
    Optional<CodeSnapshot> findFirstByProjectIdOrderByAnalyzedAtDesc(UUID projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CodeSnapshot s WHERE s.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}
