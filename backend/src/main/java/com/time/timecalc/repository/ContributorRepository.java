package com.time.timecalc.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.time.timecalc.model.Contributor;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, UUID> {
    
    // Получить всех участников конкретного проекта
    List<Contributor> findByProjectId(UUID projectId);
    
    // Найти конкретного автора в проекте по его email (нужно при парсинге Git Log)
    Optional<Contributor> findByProjectIdAndPrimaryEmail(UUID projectId, String primaryEmail);
}