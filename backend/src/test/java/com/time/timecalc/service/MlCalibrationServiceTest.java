package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.time.timecalc.dto.CalibrationResponse;
import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.MlCalibrationLog;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.json.CocomoParams;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.MlCalibrationLogRepository;
import com.time.timecalc.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class MlCalibrationServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    EstimationReportRepository reportRepository;

    @Mock
    MlCalibrationLogRepository logRepository;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    MlCalibrationService service;

    @Captor
    ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> httpEntityCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mlServiceUrl", "http://ml:5000/api/v1");
    }

    @Test
    void calibrateModel_throwsIfNotEnoughCompletedProjects() {
        when(projectRepository.findAll()).thenReturn(List.of(
                project(ProjectStatus.COMPLETED, 10.0),
                project(ProjectStatus.COMPLETED, 5.0)
        ));

        assertThatThrownBy(() -> service.calibrateModel())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("минимум 3");
    }

    @Test
    void calibrateModel_wrapsHttpErrors() {
        when(projectRepository.findAll()).thenReturn(List.of(
                project(ProjectStatus.COMPLETED, 10.0),
                project(ProjectStatus.COMPLETED, 5.0),
                project(ProjectStatus.COMPLETED, 7.0)
        ));

        for (Project p : projectRepository.findAll()) {
            when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p.getId()))
                    .thenReturn(List.of(report(10000L, 2.0, 3.0, Map.of("EM1", 1.1))));
        }

        when(restTemplate.exchange(eq("http://ml:5000/api/v1/calibrate"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(CalibrationResponse.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> service.calibrateModel())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка связи")
                .hasMessageContaining("http://ml:5000/api/v1/calibrate");
    }

    @Test
    void calibrateModel_throwsOnNullBody() {
        List<Project> projects = List.of(
                project(ProjectStatus.COMPLETED, 10.0),
                project(ProjectStatus.COMPLETED, 5.0),
                project(ProjectStatus.COMPLETED, 7.0)
        );
        when(projectRepository.findAll()).thenReturn(projects);

        for (Project p : projects) {
            when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p.getId()))
                    .thenReturn(List.of(report(10000L, 2.0, 3.0, Map.of("EM1", 1.1))));
        }

        when(restTemplate.exchange(eq("http://ml:5000/api/v1/calibrate"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(CalibrationResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> service.calibrateModel())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пустой ответ");
    }

    @Test
    void calibrateModel_throwsOnMissingFields() {
        List<Project> projects = List.of(
                project(ProjectStatus.COMPLETED, 10.0),
                project(ProjectStatus.COMPLETED, 5.0),
                project(ProjectStatus.COMPLETED, 7.0)
        );
        when(projectRepository.findAll()).thenReturn(projects);

        for (Project p : projects) {
            when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p.getId()))
                    .thenReturn(List.of(report(10000L, 2.0, 3.0, Map.of("EM1", 1.1))));
        }

        CalibrationResponse incomplete = CalibrationResponse.builder()
                .newCoefficientA(3.1)
                .build();

        when(restTemplate.exchange(eq("http://ml:5000/api/v1/calibrate"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(CalibrationResponse.class)))
                .thenReturn(ResponseEntity.ok(incomplete));

        assertThatThrownBy(() -> service.calibrateModel())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не содержит ожидаемых полей");
    }

    @Test
    void calibrateModel_success_buildsDatasetSavesLogAndReturnsPerformedAt() {
        Project p1 = project(ProjectStatus.COMPLETED, 10.0);
        Project p2 = project(ProjectStatus.COMPLETED, 5.0);
        Project p3 = project(ProjectStatus.COMPLETED, 7.0);
        when(projectRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p1.getId()))
                .thenReturn(List.of(report(12000L, 2.5, 4.0, Map.of("EM1", 1.1, "EM2", 0.9))));
        when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p2.getId()))
                .thenReturn(List.of(report(8000L, 1.5, 2.0, Map.of("EM1", 1.2))));
        when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p3.getId()))
                .thenReturn(List.of(report(30000L, 3.2, 5.0, Map.of()))); // total_em should stay 1.0

        CalibrationResponse mlResponse = CalibrationResponse.builder()
                .newCoefficientA(3.11)
                .newCoefficientB(0.88)
                .rmseScore(0.42)
                .projectsAnalyzed(3)
                .build();

        when(restTemplate.exchange(eq("http://ml:5000/api/v1/calibrate"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(CalibrationResponse.class)))
                .thenReturn(ResponseEntity.ok(mlResponse));

        LocalDateTime performedAt = LocalDateTime.of(2026, 5, 10, 12, 0);
        when(logRepository.save(any(MlCalibrationLog.class))).thenAnswer(inv -> {
            MlCalibrationLog in = inv.getArgument(0);
            in.setId(123L);
            in.setPerformedAt(performedAt);
            return in;
        });

        CalibrationResponse result = service.calibrateModel();

        verify(restTemplate).exchange(eq("http://ml:5000/api/v1/calibrate"), eq(HttpMethod.POST), httpEntityCaptor.capture(),
                eq(CalibrationResponse.class));
        List<Map<String, Object>> dataset = httpEntityCaptor.getValue().getBody();
        assertThat(dataset).isNotNull();
        assertThat(dataset).hasSize(3);

        // p1: ksloc=12, total_em=1.1*0.9=0.99, actual_effort_pm=10*4=40
        assertThat(dataset).anySatisfy(m -> {
            assertThat(m.get("ksloc")).isEqualTo(12.0);
            assertThat(m.get("avg_complexity")).isEqualTo(2.5);
            assertThat((Double) m.get("total_em")).isCloseTo(0.99, within(1e-9));
            assertThat(m.get("actual_effort_pm")).isEqualTo(40.0);
        });

        // p2: ksloc=8, total_em=1.2, actual_effort_pm=5*2=10
        assertThat(dataset).anySatisfy(m -> {
            assertThat(m.get("ksloc")).isEqualTo(8.0);
            assertThat(m.get("avg_complexity")).isEqualTo(1.5);
            assertThat((Double) m.get("total_em")).isCloseTo(1.2, within(1e-9));
            assertThat(m.get("actual_effort_pm")).isEqualTo(10.0);
        });

        // p3: ksloc=30, total_em=1.0, actual_effort_pm=7*5=35
        assertThat(dataset).anySatisfy(m -> {
            assertThat(m.get("ksloc")).isEqualTo(30.0);
            assertThat(m.get("avg_complexity")).isEqualTo(3.2);
            assertThat((Double) m.get("total_em")).isCloseTo(1.0, within(1e-9));
            assertThat(m.get("actual_effort_pm")).isEqualTo(35.0);
        });

        ArgumentCaptor<MlCalibrationLog> logCaptor = ArgumentCaptor.forClass(MlCalibrationLog.class);
        verify(logRepository).save(logCaptor.capture());
        MlCalibrationLog saved = logCaptor.getValue();
        assertThat(saved.getOldA()).isEqualTo(2.94);
        assertThat(saved.getOldB()).isEqualTo(0.91);
        assertThat(saved.getNewA()).isEqualTo(3.11);
        assertThat(saved.getNewB()).isEqualTo(0.88);
        assertThat(saved.getRmseScore()).isEqualTo(0.42);
        assertThat(saved.getProjectsUsedCount()).isEqualTo(3);

        assertThat(result.getPerformedAt()).isEqualTo(performedAt);
    }

    private static Project project(ProjectStatus status, Double actualDurationMonths) {
        return Project.builder()
                .id(UUID.randomUUID())
                .name("p")
                .repoUrl("r")
                .status(status)
                .actualDurationMonths(actualDurationMonths)
                .build();
    }

    private static EstimationReport report(long totalSloc, double avgComplexity, double teamSize, Map<String, Double> multipliers) {
        CodeSnapshot snapshot = CodeSnapshot.builder()
                .id(UUID.randomUUID())
                .totalSloc(totalSloc)
                .avgComplexity(avgComplexity)
                .build();

        CocomoParams params = CocomoParams.builder()
                .multipliers(multipliers)
                .build();

        return EstimationReport.builder()
                .snapshot(snapshot)
                .teamSize(teamSize)
                .appliedParams(params)
                .build();
    }
}

