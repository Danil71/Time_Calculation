package com.time.timecalc.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.GitCommit;

@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, String> {
    
    // Получить всю историю проекта, отсортированную от новых коммитов к старым
    List<GitCommit> findByProjectIdOrderByCommitDateDesc(UUID projectId);
    
    // Быстро посчитать общее количество коммитов в проекте
    long countByProjectId(UUID projectId);

    List<GitCommit> findByContributorId(UUID contributorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GitCommit gc set gc.contributor = :primary where gc.contributor.id = :duplicateId")
    int reassignContributor(@Param("primary") Contributor primary, @Param("duplicateId") UUID duplicateId);
}
