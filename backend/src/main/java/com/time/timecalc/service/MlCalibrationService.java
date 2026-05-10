package com.time.timecalc.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.time.timecalc.dto.CalibrationResponse;
import com.time.timecalc.model.EstimationReport;
import com.time.timecalc.model.MlCalibrationLog;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.MlCalibrationLogRepository;
import com.time.timecalc.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MlCalibrationService {

    private final ProjectRepository projectRepository;
    private final EstimationReportRepository reportRepository;
    private final MlCalibrationLogRepository logRepository;
    private final RestTemplate restTemplate;
    
    private static final double CURRENT_A = 2.94;
    private static final double CURRENT_B = 0.91;

    @Value("${application.ml-service.url:http://localhost:5000/api/v1}")
    private String mlServiceUrl;

    @Transactional
    public CalibrationResponse calibrateModel() {
        List<Project> completedProjects = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED && p.getActualDurationMonths() != null)
                .toList();

        if (completedProjects.size() < 3) {
            throw new RuntimeException("Для калибровки нужно минимум 3 завершенных проекта с указанными фактическими сроками.");
        }

        List<Map<String, Object>> dataset = new ArrayList<>();

        for (Project p : completedProjects) {
            List<EstimationReport> reports = reportRepository.findBySnapshotProjectIdOrderByCalculatedAtDesc(p.getId());
            if (reports.isEmpty()) continue;

            EstimationReport lastReport = reports.get(0);
            
            double totalEm = 1.0;
            if (lastReport.getAppliedParams() != null && lastReport.getAppliedParams().getMultipliers() != null) {
                for (Double val : lastReport.getAppliedParams().getMultipliers().values()) {
                    totalEm *= val;
                }
            }

            double actualEffortPm = p.getActualDurationMonths() * lastReport.getTeamSize();

            Map<String, Object> projectData = new HashMap<>();
            projectData.put("ksloc", lastReport.getSnapshot().getTotalSloc() / 1000.0);
            projectData.put("avg_complexity", lastReport.getSnapshot().getAvgComplexity());
            projectData.put("total_em", totalEm);
            projectData.put("actual_effort_pm", actualEffortPm);

            dataset.add(projectData);
        }

        String targetUrl = mlServiceUrl + "/calibrate";
        
        ResponseEntity<CalibrationResponse> response;
        try {
            HttpEntity<List<Map<String, Object>>> requestEntity = new HttpEntity<>(dataset);
            response = restTemplate.exchange(targetUrl, HttpMethod.POST, requestEntity, CalibrationResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка связи с сервисом машинного обучения (" + targetUrl + "): " + e.getMessage());
        }

        CalibrationResponse mlResult = response.getBody();
        if (mlResult == null) {
            throw new RuntimeException("Пустой ответ от сервиса машинного обучения (" + targetUrl + ").");
        }
        if (mlResult.getNewCoefficientA() == null || mlResult.getNewCoefficientB() == null
                || mlResult.getRmseScore() == null || mlResult.getProjectsAnalyzed() == null) {
            throw new RuntimeException(
                    "Ответ ML-сервиса не содержит ожидаемых полей (new_a, new_b, rmse, projects_analyzed).");
        }

        MlCalibrationLog log = MlCalibrationLog.builder()
                .oldA(CURRENT_A)
                .oldB(CURRENT_B)
                .newA(mlResult.getNewCoefficientA())
                .newB(mlResult.getNewCoefficientB())
                .rmseScore(mlResult.getRmseScore())
                .projectsUsedCount(mlResult.getProjectsAnalyzed())
                .build();
        log = logRepository.save(log);
        mlResult.setPerformedAt(log.getPerformedAt());

        return mlResult;
    }
}