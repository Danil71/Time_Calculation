package com.time.timecalc.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContributorResponse {
    private UUID id;
    private String primaryEmail;
    private String displayName;
    private Integer totalCommits;
    private Double churnFactor;
    private Double persRating; // Итоговый коэффициент квалификации
    private List<String> aliases;
}
