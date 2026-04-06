package com.time.timecalc.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.time.timecalc.dto.EstimationResponse;
import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.ProgrammingLanguage;
import com.time.timecalc.model.json.CocomoParams;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.ProgrammingLanguageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CocomoEngineService {

    private final CodeSnapshotRepository snapshotRepository;
    private final EstimationReportRepository reportRepository;
    private final ProgrammingLanguageRepository programmingLanguageRepository;

    private static final double A_CONSTANT = 2.94;
    private static final double B_CONSTANT = 0.91;

    @Transactional
    public EstimationResponse calculateEstimation(UUID projectId, Map<String, Double> targetFpDetails) {
        CodeSnapshot snapshot = snapshotRepository.findFirstByProjectIdOrderByAnalyzedAtDesc(projectId)
        .orElseThrow(() -> new RuntimeException("Сначала проанализируйте код"));

        long slocToCalculate = snapshot.getTotalSloc(); // По умолчанию Аудит

        // МАГИЯ: СУММИРУЕМ SLOC ПО КАЖДОМУ ЯЗЫКУ ОТДЕЛЬНО!
        if (targetFpDetails != null && !targetFpDetails.isEmpty()) {
            long calculatedSloc = 0;
            
            for (Map.Entry<String, Double> entry : targetFpDetails.entrySet()) {
                String langName = entry.getKey();
                Double fp = entry.getValue();
                
                // Достаем плотность языка из БД (если нет, берем среднее 50)
                int locPerFp = programmingLanguageRepository.findById(langName)
                        .map(ProgrammingLanguage::getLocPerFp)
                        .orElse(50); 
                
                calculatedSloc += (long) (fp * locPerFp);
            }
            slocToCalculate = calculatedSloc;
        }

        double ksloc = slocToCalculate / 1000.0;
        if (ksloc <= 0) ksloc = 0.1;

        double scaleFactorE = B_CONSTANT + (0.01 * snapshot.getAvgComplexity());

        Map<String, Double> multipliers = new HashMap<>();
        double totalEm = 1.0;

        double churnRisk = 1.0;
        if (snapshot.getChurnRate() != null) {
            if (snapshot.getChurnRate() > 0.4) churnRisk = 1.30;
            else if (snapshot.getChurnRate() > 0.2) churnRisk = 1.15;
        }
        multipliers.put("CHURN_RISK", churnRisk);
        totalEm *= churnRisk;

        double effortPm = A_CONSTANT * Math.pow(ksloc, scaleFactorE) * totalEm;

        double cConstant = 3.67;
        double fExponent = 0.28 + 0.2 * (scaleFactorE - B_CONSTANT);
        double durationMonths = cConstant * Math.pow(effortPm, fExponent);

        // 6. Оптимальная команда
        double teamSize = effortPm / durationMonths;

        // Сохраняем параметры формулы
        CocomoParams params = CocomoParams.builder()
                .coefficientA(A_CONSTANT)
                .coefficientB(scaleFactorE)
                .multipliers(multipliers)
                .build();

        EstimationReport report = EstimationReport.builder()
                .snapshot(snapshot)
                .targetFpDetails(targetFpDetails)
                .effortPm(effortPm)
                .durationMonths(durationMonths)
                .teamSize(teamSize)
                .appliedParams(params)
                .build();

        report = reportRepository.save(report);

        return mapToResponse(report, snapshot);
    }

    @Transactional(readOnly = true)
    public List<EstimationResponse> getHistory(UUID projectId) {
        return reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(projectId).stream()
                .map(report -> mapToResponse(report, report.getSnapshot()))
                .collect(Collectors.toList());
    }

    private EstimationResponse mapToResponse(EstimationReport report, CodeSnapshot snapshot) {
        return EstimationResponse.builder()
                .reportId(report.getId())
                .calculatedAt(report.getCalculatedAt())
                .effortPm(report.getEffortPm())
                .durationMonths(report.getDurationMonths())
                .recommendedTeam(report.getTeamSize())
                .totalSloc(snapshot.getTotalSloc())
                .avgComplexity(snapshot.getAvgComplexity())
                .churnRate(snapshot.getChurnRate())
                .techStack(snapshot.getTechStack())
                .build();
    }
}
