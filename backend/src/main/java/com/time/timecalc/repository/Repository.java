package com.time.timecalc.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.time.timecalc.model.Project;
import com.time.timecalc.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByRepoUrl(String repoUrl);
}

public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, UUID> {
    // Найти последний анализ для проекта
    Optional<CodeSnapshot> findFirstByProjectIdOrderByAnalyzedAtDesc(UUID projectId);
}

public interface EstimationReportRepository extends JpaRepository<EstimationReport, UUID> {
    // История отчетов проекта
    List<EstimationReport> findBySnapshotProjectIdOrderByCalculatedAtDesc(UUID projectId);
}