package com.time.timecalc.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.time.timecalc.model.EstimationReport;

public interface EstimationReportRepository extends JpaRepository<EstimationReport, UUID> {
    // История отчетов проекта
    List<EstimationReport> findBySnapshotProjectIdOrderByCalculatedAtDesc(UUID projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EstimationReport r WHERE r.snapshot.project.id = :projectId")
    void deleteAllBySnapshotProjectId(@Param("projectId") UUID projectId);
}
