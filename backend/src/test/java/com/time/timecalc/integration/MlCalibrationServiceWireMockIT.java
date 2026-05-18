package com.time.timecalc.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.time.timecalc.dto.CalibrationResponse;
import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.MlCalibrationLog;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.enums.RiskLevel;
import com.time.timecalc.model.json.CocomoParams;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.MlCalibrationLogRepository;
import com.time.timecalc.repository.ProjectRepository;
import com.time.timecalc.service.MlCalibrationService;

/**
 * Реальный {@link org.springframework.web.client.RestTemplate} + ответ ML-сервиса через WireMock.
 * Проверяем POST /calibrate, десериализацию JSON (поля new_a, new_b, rmse, projects_analyzed) и запись {@link MlCalibrationLog}.
 */
@Transactional
class MlCalibrationServiceWireMockIT extends AbstractIntegrationTest {

    private static final WireMockServer ML_WIREMOCK = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        ML_WIREMOCK.start();
    }

    @DynamicPropertySource
    static void registerMlServiceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("application.ml-service.url", () -> "http://localhost:" + ML_WIREMOCK.port() + "/api/v1");
    }

    @Autowired
    private MlCalibrationService mlCalibrationService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CodeSnapshotRepository codeSnapshotRepository;

    @Autowired
    private EstimationReportRepository estimationReportRepository;

    @Autowired
    private MlCalibrationLogRepository mlCalibrationLogRepository;

    @BeforeEach
    void resetWireMock() {
        ML_WIREMOCK.resetAll();
    }

    @Test
    @DisplayName("Калибровка: RestTemplate шлёт JSON на ML, парсит ответ и сохраняет MlCalibrationLog")
    void calibrateModel_wireMockMlResponse_isPersisted() {
        ML_WIREMOCK.stubFor(post(urlPathEqualTo("/api/v1/calibrate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"new_a\":3.14,\"new_b\":1.01,\"rmse\":0.77,\"projects_analyzed\":3}")));

        seedThreeCompletedProjectsWithReports();

        CalibrationResponse response = mlCalibrationService.calibrateModel();

        assertThat(response.getNewCoefficientA()).isEqualTo(3.14);
        assertThat(response.getNewCoefficientB()).isEqualTo(1.01);
        assertThat(response.getRmseScore()).isEqualTo(0.77);
        assertThat(response.getProjectsAnalyzed()).isEqualTo(3);
        assertThat(response.getPerformedAt()).isNotNull();

        MlCalibrationLog latest = mlCalibrationLogRepository.findFirstByOrderByPerformedAtDesc().orElseThrow();
        assertThat(latest.getNewA()).isEqualTo(3.14);
        assertThat(latest.getNewB()).isEqualTo(1.01);
        assertThat(latest.getRmseScore()).isEqualTo(0.77);
        assertThat(latest.getProjectsUsedCount()).isEqualTo(3);

        ML_WIREMOCK.verify(postRequestedFor(urlPathEqualTo("/api/v1/calibrate")));
    }

    private void seedThreeCompletedProjectsWithReports() {
        for (int i = 0; i < 3; i++) {
            Project project = projectRepository.save(Project.builder()
                    .name("ML dataset " + i)
                    .repoUrl("https://example.com/ml-" + i + "-" + UUID.randomUUID() + ".git")
                    .branchName("main")
                    .status(ProjectStatus.COMPLETED)
                    .currentRiskLevel(RiskLevel.LOW)
                    .actualDurationMonths(6.0 + i)
                    .build());

            String hex = UUID.randomUUID().toString().replace("-", "");
            String hash = (hex + hex).substring(0, 40);

            CodeSnapshot snapshot = codeSnapshotRepository.save(CodeSnapshot.builder()
                    .project(project)
                    .commitHash(hash)
                    .totalSloc(5000L + 100L * i)
                    .avgComplexity(5.0)
                    .churnRate(0.1)
                    .build());

            estimationReportRepository.save(EstimationReport.builder()
                    .snapshot(snapshot)
                    .effortPm(100.0)
                    .durationMonths(5.0)
                    .teamSize(3.0)
                    .appliedParams(CocomoParams.builder()
                            .coefficientA(2.94)
                            .coefficientB(0.91)
                            .multipliers(Map.of("CHURN_RISK", 1.1))
                            .build())
                    .build());
        }
    }
}
