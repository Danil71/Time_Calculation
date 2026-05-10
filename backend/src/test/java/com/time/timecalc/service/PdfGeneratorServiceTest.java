package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.Project;
import com.time.timecalc.repository.EstimationReportRepository;

@ExtendWith(MockitoExtension.class)
class PdfGeneratorServiceTest {

    @Mock
    EstimationReportRepository reportRepository;

    @InjectMocks
    PdfGeneratorService service;

    @Test
    void generateEstimationReportPdf_throwsIfReportNotFound() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateEstimationReportPdf(reportId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Отчет не найден");
    }

    @Test
    void generateEstimationReportPdf_returnsNonEmptyPdfBytes() {
        UUID reportId = UUID.randomUUID();

        Project project = Project.builder()
                .id(UUID.randomUUID())
                .name("P")
                .repoUrl("url")
                .branchName("main")
                .build();

        CodeSnapshot snapshot = CodeSnapshot.builder()
                .id(UUID.randomUUID())
                .project(project)
                .analyzedAt(LocalDateTime.of(2026, 5, 10, 12, 0))
                .totalSloc(1000L)
                .avgComplexity(2.0)
                .churnRate(0.1)
                .build();

        EstimationReport report = EstimationReport.builder()
                .id(reportId)
                .snapshot(snapshot)
                .effortPm(12.34)
                .durationMonths(5.67)
                .teamSize(2.2)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        byte[] pdf = service.generateEstimationReportPdf(reportId);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}

