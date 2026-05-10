package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.time.timecalc.dto.EstimationResponse;
import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.MlCalibrationLog;
import com.time.timecalc.model.ProgrammingLanguage;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.MlCalibrationLogRepository;
import com.time.timecalc.repository.ProgrammingLanguageRepository;

@ExtendWith(MockitoExtension.class)
class CocomoEngineServiceTest {

    @Mock
    CodeSnapshotRepository snapshotRepository;

    @Mock
    EstimationReportRepository reportRepository;

    @Mock
    ProgrammingLanguageRepository programmingLanguageRepository;

    @Mock
    MlCalibrationLogRepository mlLogRepository;

    @InjectMocks
    CocomoEngineService service;

    @Test
    void calculateEstimation_throwsIfNoSnapshot() {
        UUID projectId = UUID.randomUUID();
        when(snapshotRepository.findFirstByProjectIdOrderByAnalyzedAtDesc(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateEstimation(projectId, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("проанализируйте код");
    }

    @Test
    void calculateEstimation_usesLatestMlCalibrationCoefficients_whenPresent() {
        UUID projectId = UUID.randomUUID();
        CodeSnapshot snapshot = CodeSnapshot.builder()
                .id(UUID.randomUUID())
                .totalSloc(10000L)
                .avgComplexity(2.0)
                .churnRate(0.0)
                .techStack(Map.of("Java", 10000L))
                .build();

        when(snapshotRepository.findFirstByProjectIdOrderByAnalyzedAtDesc(projectId)).thenReturn(Optional.of(snapshot));
        when(mlLogRepository.findFirstByOrderByPerformedAtDesc()).thenReturn(Optional.of(
                MlCalibrationLog.builder().newA(4.0).newB(1.0).build()
        ));

        when(reportRepository.save(any(EstimationReport.class))).thenAnswer(inv -> {
            EstimationReport r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        EstimationResponse res = service.calculateEstimation(projectId, null);

        assertThat(res.getTotalSloc()).isEqualTo(10000L);
        assertThat(res.getEffortPm()).isNotNull();
        assertThat(res.getDurationMonths()).isNotNull();
        assertThat(res.getRecommendedTeam()).isNotNull();

        ArgumentCaptor<EstimationReport> reportCaptor = ArgumentCaptor.forClass(EstimationReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        EstimationReport saved = reportCaptor.getValue();

        // coefficientA should come from latest log
        assertThat(saved.getAppliedParams().getCoefficientA()).isEqualTo(4.0);
        // coefficientB is stored as scaleFactorE (currentB + 0.01 * avgComplexity)
        assertThat(saved.getAppliedParams().getCoefficientB()).isCloseTo(1.0 + 0.01 * 2.0, within(1e-12));
        assertThat(saved.getAppliedParams().getMultipliers()).containsKey("CHURN_RISK");
    }

    @Test
    void calculateEstimation_recalculatesSlocFromTargetFpDetails_withDefaultLocPerFp50() {
        UUID projectId = UUID.randomUUID();
        CodeSnapshot snapshot = CodeSnapshot.builder()
                .id(UUID.randomUUID())
                .totalSloc(999999L)
                .avgComplexity(1.0)
                .churnRate(0.0)
                .techStack(Map.of())
                .build();
        when(snapshotRepository.findFirstByProjectIdOrderByAnalyzedAtDesc(projectId)).thenReturn(Optional.of(snapshot));
        when(mlLogRepository.findFirstByOrderByPerformedAtDesc()).thenReturn(Optional.empty());

        // One language exists, the other doesn't -> default 50
        when(programmingLanguageRepository.findById("Java"))
                .thenReturn(Optional.of(ProgrammingLanguage.builder().name("Java").locPerFp(53).build()));
        when(programmingLanguageRepository.findById("Python")).thenReturn(Optional.empty());

        when(reportRepository.save(any(EstimationReport.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Double> target = Map.of("Java", 10.0, "Python", 5.0);
        service.calculateEstimation(projectId, target);

        ArgumentCaptor<EstimationReport> reportCaptor = ArgumentCaptor.forClass(EstimationReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        EstimationReport saved = reportCaptor.getValue();

        // slocToCalculate = 10*53 + 5*50 = 780
        assertThat(saved.getSnapshot().getTotalSloc()).isEqualTo(999999L); // snapshot itself unchanged
        assertThat(saved.getTargetFpDetails()).isEqualTo(target);
        assertThat(saved.getEffortPm()).isGreaterThan(0.0);
    }

    @Test
    void getHistory_mapsReportsToResponses() {
        UUID projectId = UUID.randomUUID();
        CodeSnapshot snapshot = CodeSnapshot.builder()
                .id(UUID.randomUUID())
                .totalSloc(1000L)
                .avgComplexity(1.0)
                .churnRate(0.1)
                .techStack(Map.of("Java", 1000L))
                .build();
        EstimationReport r = EstimationReport.builder()
                .id(UUID.randomUUID())
                .snapshot(snapshot)
                .effortPm(1.0)
                .durationMonths(2.0)
                .teamSize(0.5)
                .build();

        when(reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(projectId)).thenReturn(List.of(r));

        List<EstimationResponse> history = service.getHistory(projectId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getTotalSloc()).isEqualTo(1000L);
        assertThat(history.get(0).getEffortPm()).isEqualTo(1.0);
    }
}

