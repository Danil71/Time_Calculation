package com.time.timecalc.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.dto.CalculateRequest;
import com.time.timecalc.dto.EstimationResponse;
import com.time.timecalc.service.CocomoEngineService;
import com.time.timecalc.service.PdfGeneratorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estimations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstimationController {

    private final CocomoEngineService cocomoService;
    private final PdfGeneratorService pdfGeneratorService;
    @GetMapping("/{projectId}/history")
    public ResponseEntity<List<EstimationResponse>> getEstimationHistory(@PathVariable UUID projectId) {
        return ResponseEntity.ok(cocomoService.getHistory(projectId));
    }

    @PostMapping("/{projectId}/calculate")
    public ResponseEntity<EstimationResponse> calculateEstimation(
            @PathVariable UUID projectId,
            @RequestBody(required = false) CalculateRequest request) {
        
        Map<String, Double> fpDetails = (request != null) ? request.getTargetFpDetails() : null;
        return ResponseEntity.ok(cocomoService.calculateEstimation(projectId, fpDetails));
    }

    @GetMapping("/{reportId}/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable UUID reportId) {
        byte[] pdfBytes = pdfGeneratorService.generateEstimationReportPdf(reportId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // Заголовок attachment заставляет браузер скачать файл, а не открыть его как страницу
        headers.setContentDispositionFormData("attachment", "estimation_report_" + reportId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
