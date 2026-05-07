package com.time.timecalc.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationResponse {
    
    private LocalDateTime performedAt;
    
    private Double rmseScore;
    
    private Double newCoefficientA;
    private Double newCoefficientB;
    
    private Integer projectsAnalyzed;
}