package com.time.timecalc.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.time.timecalc.model.EstimationReport;

public interface EstimationReportRepository extends JpaRepository<EstimationReport, UUID> {
    // История отчетов проекта
    List<EstimationReport> findBySnapshotProjectIdOrderByCalculatedAtDesc(UUID projectId);
}
