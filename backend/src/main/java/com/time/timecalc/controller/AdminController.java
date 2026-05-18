package com.time.timecalc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.dto.CalibrationResponse;
import com.time.timecalc.service.MlCalibrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final MlCalibrationService mlCalibrationService;

    @PostMapping("/calibrate-model")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalibrationResponse> runCalibration() {
        return ResponseEntity.ok(mlCalibrationService.calibrateModel());
    }
}
