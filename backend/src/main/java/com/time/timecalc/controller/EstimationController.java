package com.time.timecalc.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.dto.EstimationResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estimations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstimationController {

    private final CocomoEngineService cocomoService;

    @PostMapping("/{projectId}/calculate")
    public ResponseEntity<EstimationResponse> calculateEstimation(@PathVariable UUID projectId) {
        return ResponseEntity.ok(cocomoService.calculateEstimation(projectId));
    }

    @GetMapping("/{projectId}/history")
    public ResponseEntity<List<EstimationResponse>> getEstimationHistory(@PathVariable UUID projectId) {
        return ResponseEntity.ok(cocomoService.getHistory(projectId));
    }
}
