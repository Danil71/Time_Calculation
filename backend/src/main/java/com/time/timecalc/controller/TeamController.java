package com.time.timecalc.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.dto.ContributorResponse;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.service.TeamProfiler;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/team")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeamController {

    private final ContributorRepository contributorRepository;
    private final TeamProfiler teamProfiler;

    @GetMapping
    public ResponseEntity<List<ContributorResponse>> getTeam(@PathVariable UUID projectId) {
        List<ContributorResponse> team = contributorRepository.findByProjectId(projectId).stream()
                .map(c -> ContributorResponse.builder()
                        .id(c.getId())
                        .primaryEmail(c.getPrimaryEmail())
                        .displayName(c.getDisplayName())
                        .totalCommits(c.getTotalCommits())
                        // Защита от null
                        .churnFactor(c.getChurnFactor() != null ? c.getChurnFactor() : 0.0) 
                        // Берем итоговый фактор (с учетом возможных ручных правок менеджера)
                        .persRating(c.getEffectivePersFactor() != null ? c.getEffectivePersFactor() : 1.0)
                        .aliases(c.getAliases())
                        .build())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(team);
    }

    // Склеить дубликаты (Алиасы)
    @PostMapping("/merge")
    public ResponseEntity<String> mergeContributors(
            @PathVariable UUID projectId,
            @RequestParam UUID primaryId,
            @RequestParam UUID duplicateId) {
        
        teamProfiler.mergeAliases(primaryId, duplicateId);
        return ResponseEntity.ok("Профили успешно объединены");
    }
}
