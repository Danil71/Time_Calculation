package com.time.timecalc.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.GitCommit;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.enums.RiskLevel;
import com.time.timecalc.model.json.CocomoParams;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.GitCommitRepository;
import com.time.timecalc.repository.ProjectRepository;
import com.time.timecalc.service.ProjectService;

import jakarta.persistence.EntityManager;

/**
 * Data JPA + PostgreSQL: проверяем маппинг JSONB и фактическое удаление связанных строк при удалении проекта
 * (каскад на коммиты/участников и явные delete в {@link ProjectService} для снимков и отчётов).
 */
@Transactional
class JsonbAndCascadePersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CodeSnapshotRepository codeSnapshotRepository;

    @Autowired
    private EstimationReportRepository estimationReportRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private GitCommitRepository gitCommitRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("JSONB: tech_stack, aliases, applied_params и target_fp_details читаются из PostgreSQL без потери данных")
    void jsonbFields_roundTripThroughPostgres() {
        Project project = projectRepository.save(Project.builder()
                .name("JSONB Demo")
                .repoUrl("https://example.com/repo.git")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build());

        Map<String, Long> techStack = new HashMap<>();
        techStack.put("Java", 1200L);
        techStack.put("Kotlin", 300L);

        CodeSnapshot snapshot = codeSnapshotRepository.save(CodeSnapshot.builder()
                .project(project)
                .commitHash("a".repeat(40))
                .totalSloc(1500L)
                .avgComplexity(4.2)
                .churnRate(0.11)
                .techStack(techStack)
                .build());

        Map<String, Double> multipliers = Map.of("CHURN_RISK", 1.15, "TEAM", 1.05);
        CocomoParams applied = CocomoParams.builder()
                .coefficientA(2.94)
                .coefficientB(0.95)
                .multipliers(multipliers)
                .build();

        Map<String, Double> fpDetails = Map.of("Java", 12.0, "SQL", 3.0);

        EstimationReport report = estimationReportRepository.save(EstimationReport.builder()
                .snapshot(snapshot)
                .effortPm(42.0)
                .durationMonths(5.0)
                .teamSize(4.0)
                .appliedParams(applied)
                .targetFpDetails(fpDetails)
                .build());

        entityManager.flush();
        entityManager.clear();

        EstimationReport loaded = estimationReportRepository.findById(report.getId()).orElseThrow();
        assertThat(loaded.getAppliedParams().getCoefficientA()).isEqualTo(2.94);
        assertThat(loaded.getAppliedParams().getMultipliers()).containsEntry("CHURN_RISK", 1.15);
        assertThat(loaded.getTargetFpDetails()).containsEntry("Java", 12.0);

        CodeSnapshot loadedSnap = loaded.getSnapshot();
        assertThat(loadedSnap.getTechStack()).containsEntry("Java", 1200L);

        Contributor contributor = contributorRepository.save(Contributor.builder()
                .project(project)
                .primaryEmail("dev@example.com")
                .displayName("Dev")
                .aliases(List.of("alias1@example.com", "alias2@example.com"))
                .firstCommitAt(LocalDateTime.now().minusMonths(6))
                .lastCommitAt(LocalDateTime.now())
                .totalCommits(10)
                .churnFactor(0.2)
                .calculatedPersFactor(1.0)
                .build());

        entityManager.flush();
        entityManager.clear();

        Contributor loadedContributor = contributorRepository.findById(contributor.getId()).orElseThrow();
        assertThat(loadedContributor.getAliases()).containsExactly("alias1@example.com", "alias2@example.com");
    }

    @Test
    @DisplayName("Удаление проекта: снимки, отчёты, коммиты и участники исчезают из БД")
    void deleteProject_removesSnapshotsReportsCommitsAndContributors() {
        Project project = projectRepository.save(Project.builder()
                .name("Cascade Demo")
                .repoUrl("https://example.com/cascade-" + UUID.randomUUID() + ".git")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build());

        Contributor contributor = contributorRepository.save(Contributor.builder()
                .project(project)
                .primaryEmail("c1@example.com")
                .displayName("C1")
                .aliases(List.of("old@example.com"))
                .firstCommitAt(LocalDateTime.now().minusDays(30))
                .lastCommitAt(LocalDateTime.now())
                .totalCommits(3)
                .churnFactor(0.1)
                .calculatedPersFactor(1.0)
                .build());

        gitCommitRepository.save(GitCommit.builder()
                .hash("b".repeat(40))
                .project(project)
                .contributor(contributor)
                .commitDate(LocalDateTime.now())
                .message("init")
                .linesAdded(10)
                .linesDeleted(2)
                .filesChanged(1)
                .isMerge(false)
                .build());

        CodeSnapshot snapshot = codeSnapshotRepository.save(CodeSnapshot.builder()
                .project(project)
                .commitHash("c".repeat(40))
                .totalSloc(800L)
                .avgComplexity(3.0)
                .churnRate(0.05)
                .techStack(Map.of("Java", 800L))
                .build());

        estimationReportRepository.save(EstimationReport.builder()
                .snapshot(snapshot)
                .effortPm(10.0)
                .durationMonths(2.0)
                .teamSize(2.0)
                .appliedParams(CocomoParams.builder().coefficientA(2.0).coefficientB(0.9).multipliers(Map.of()).build())
                .build());

        entityManager.flush();
        UUID projectId = project.getId();
        UUID contributorId = contributor.getId();
        String commitHash = "b".repeat(40);
        UUID snapshotId = snapshot.getId();

        projectService.deleteProject(projectId);
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(contributorRepository.findById(contributorId)).isEmpty();
        assertThat(gitCommitRepository.findById(commitHash)).isEmpty();
        assertThat(codeSnapshotRepository.findById(snapshotId)).isEmpty();
        assertThat(estimationReportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(projectId)).isEmpty();
    }
}
