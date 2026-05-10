package com.time.timecalc.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

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

    @JsonAlias("rmse")
    private Double rmseScore;

    @JsonAlias("new_a")
    private Double newCoefficientA;

    @JsonAlias("new_b")
    private Double newCoefficientB;

    @JsonAlias("projects_analyzed")
    private Integer projectsAnalyzed;
}