package com.time.timecalc.model.json;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocomoParams {
    private Double coefficientA;
    private Double coefficientB;
    private Map<String, Double> multipliers;
}
