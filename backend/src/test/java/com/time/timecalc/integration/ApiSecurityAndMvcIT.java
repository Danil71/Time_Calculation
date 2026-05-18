package com.time.timecalc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.time.timecalc.dto.ProjectRequest;
import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.GitCommit;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.enums.RiskLevel;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.GitCommitRepository;
import com.time.timecalc.repository.ProjectRepository;

import jakarta.persistence.EntityManager;

/**
 * MockMvc + реальный контекст: RBAC через {@link WithMockUser} и «счастливые» сценарии с проверкой БД.
 */
@Transactional
class ApiSecurityAndMvcIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private GitCommitRepository gitCommitRepository;

    @Autowired
    private CodeSnapshotRepository codeSnapshotRepository;

    @Autowired
    private EstimationReportRepository estimationReportRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @WithMockUser(roles = "DEVELOPER")
    @DisplayName("RBAC: список проектов доступен любой аутентифицированной роли (нет ограничения MANAGER)")
    void listProjects_asDeveloper_returns200() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Без JWT: защищённый GET /api/projects → 401 (аноним не аутентифицирован)")
    void protectedEndpoint_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    @DisplayName("RBAC: разработчик не может создать проект (только MANAGER/ADMIN)")
    void createProject_asDeveloper_returns403() throws Exception {
        ProjectRequest body = new ProjectRequest();
        body.setName("Forbidden");
        body.setRepoUrl("https://example.com/forbidden-" + UUID.randomUUID() + ".git");
        body.setBranchName("main");

        mockMvc.perform(post("/api/projects")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    @DisplayName("RBAC: POST /api/git/.../analyze только для MANAGER/ADMIN")
    void gitAnalyze_asDeveloper_returns403() throws Exception {
        mockMvc.perform(post("/api/git/" + UUID.randomUUID() + "/analyze"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("RBAC: калибровка ML только для ADMIN")
    void calibrateModel_asManager_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/calibrate-model"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Happy path: создание проекта через API сохраняет сущность в БД")
    void createProject_asManager_persistsProject() throws Exception {
        String repoUrl = "https://example.com/manager-" + UUID.randomUUID() + ".git";
        ProjectRequest body = new ProjectRequest();
        body.setName("Manager Project");
        body.setRepoUrl(repoUrl);
        body.setBranchName("develop");

        mockMvc.perform(post("/api/projects")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repoUrl").value(repoUrl))
                .andExpect(jsonPath("$.name").value("Manager Project"));

        assertThat(projectRepository.findAll().stream().anyMatch(p -> repoUrl.equals(p.getRepoUrl()))).isTrue();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Happy path: merge участников обновляет алиасы и удаляет дубликат в БД")
    void mergeTeam_asManager_mergesContributorsInDatabase() throws Exception {
        Project project = projectRepository.save(Project.builder()
                .name("Team merge")
                .repoUrl("https://example.com/team-" + UUID.randomUUID() + ".git")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build());

        Contributor primary = contributorRepository.save(Contributor.builder()
                .project(project)
                .primaryEmail("lead@example.com")
                .displayName("Lead")
                .aliases(List.of("lead@alias.com"))
                .firstCommitAt(LocalDateTime.now().minusDays(10))
                .lastCommitAt(LocalDateTime.now())
                .totalCommits(2)
                .churnFactor(0.1)
                .calculatedPersFactor(1.0)
                .build());

        Contributor duplicate = contributorRepository.save(Contributor.builder()
                .project(project)
                .primaryEmail("dup@example.com")
                .displayName("Dup")
                .aliases(List.of())
                .firstCommitAt(LocalDateTime.now().minusDays(5))
                .lastCommitAt(LocalDateTime.now())
                .totalCommits(3)
                .churnFactor(0.2)
                .calculatedPersFactor(1.0)
                .build());

        gitCommitRepository.save(GitCommit.builder()
                .hash("d".repeat(40))
                .project(project)
                .contributor(duplicate)
                .commitDate(LocalDateTime.now())
                .message("c")
                .linesAdded(5)
                .linesDeleted(1)
                .filesChanged(1)
                .isMerge(false)
                .build());

        entityManager.flush();

        mockMvc.perform(post("/api/projects/{projectId}/team/merge", project.getId())
                        .param("primaryId", primary.getId().toString())
                        .param("duplicateId", duplicate.getId().toString()))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        assertThat(contributorRepository.findById(duplicate.getId())).isEmpty();
        Contributor merged = contributorRepository.findById(primary.getId()).orElseThrow();
        assertThat(merged.getAliases()).contains("dup@example.com", "lead@alias.com");
        assertThat(merged.getTotalCommits()).isEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    @DisplayName("Happy path: расчёт оценки доступен разработчику и создаёт EstimationReport в БД")
    void calculateEstimation_asDeveloper_createsReport() throws Exception {
        Project project = projectRepository.save(Project.builder()
                .name("Estimation")
                .repoUrl("https://example.com/est-" + UUID.randomUUID() + ".git")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build());

        codeSnapshotRepository.save(CodeSnapshot.builder()
                .project(project)
                .commitHash("e".repeat(40))
                .totalSloc(10_000L)
                .avgComplexity(5.0)
                .churnRate(0.1)
                .build());

        entityManager.flush();
        long reportsBefore = estimationReportRepository.count();

        mockMvc.perform(post("/api/estimations/{projectId}/calculate", project.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSloc").value(10_000));

        assertThat(estimationReportRepository.count()).isEqualTo(reportsBefore + 1);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Happy path: завершение проекта (фактические месяцы) обновляет статус в БД")
    void completeProject_asManager_updatesDatabase() throws Exception {
        Project project = projectRepository.save(Project.builder()
                .name("Complete me")
                .repoUrl("https://example.com/complete-" + UUID.randomUUID() + ".git")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build());

        mockMvc.perform(put("/api/projects/{id}/complete", project.getId())
                        .param("actualDurationMonths", "8.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        entityManager.flush();
        entityManager.clear();

        Project reloaded = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(reloaded.getActualDurationMonths()).isEqualTo(8.5);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Happy path: DELETE проекта удаляет запись из БД")
    void deleteProject_asManager_removesFromDatabase() throws Exception {
        Project project = projectRepository.save(Project.builder()
                .name("Delete me")
                .repoUrl("https://example.com/del-" + UUID.randomUUID() + ".git")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build());

        mockMvc.perform(delete("/api/projects/{id}", project.getId()))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(project.getId())).isEmpty();
    }
}
