package com.time.timecalc.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.enums.RiskLevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResponse {
    private UUID id;
    private String name;
    private String repoUrl;
    private String branchName;
    private ProjectStatus status;
    private RiskLevel currentRiskLevel;
    private Double actualDurationMonths; // Факт (если проект завершен)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
