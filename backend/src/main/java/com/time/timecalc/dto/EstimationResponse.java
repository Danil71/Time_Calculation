package com.time.timecalc.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstimationResponse {
    private UUID reportId;
    private LocalDateTime calculatedAt;
    
    // Результаты COCOMO II
    private Double effortPm;        // Человеко-месяцы
    private Double durationMonths;  // Календарные месяцы
    private Double recommendedTeam; // Размер команды
    
    // Технические метрики из среза кода (Snapshot)
    private Long totalSloc;
    private Double avgComplexity;
    private Double churnRate;
    
    // Распределение по языкам (JSONB)
    private Map<String, Long> techStack;
}