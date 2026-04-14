package com.time.timecalc.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.time.timecalc.dto.ProjectRequest;
import com.time.timecalc.dto.ProjectResponse;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.enums.RiskLevel;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EstimationReportRepository estimationReportRepository;
    private final CodeSnapshotRepository codeSnapshotRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        
        String targetBranch = (request.getBranchName() != null && !request.getBranchName().trim().isEmpty()) 
                ? request.getBranchName().trim() 
                : "main";

        if (projectRepository.existsByRepoUrlAndBranchName(request.getRepoUrl(), targetBranch)) {
            throw new RuntimeException("Этот репозиторий с веткой '" + targetBranch + "' уже добавлен в систему");
        }

        Project project = Project.builder()
                .name(request.getName())
                .repoUrl(request.getRepoUrl())
                .branchName(targetBranch)
                .authTokenEnc(request.getToken())
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build();

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse setActualDuration(UUID id, Double durationMonths) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));
        
        project.setActualDurationMonths(durationMonths);
        project.setStatus(ProjectStatus.COMPLETED);
        
        return mapToResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(UUID id) {
        estimationReportRepository.deleteAllBySnapshotProjectId(id);
        codeSnapshotRepository.deleteAllByProjectId(id);
        projectRepository.deleteById(id);
    }

    private ProjectResponse mapToResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .repoUrl(p.getRepoUrl())
                .branchName(p.getBranchName())
                .status(p.getStatus())
                .currentRiskLevel(p.getCurrentRiskLevel())
                .actualDurationMonths(p.getActualDurationMonths())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
