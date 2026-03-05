package com.time.timecalc.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contributors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contributor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "primary_email", nullable = false)
    private String primaryEmail;

    @Column(name = "display_name")
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> aliases;

    @Column(name = "first_commit_at")
    private LocalDateTime firstCommitAt;

    @Column(name = "last_commit_at")
    private LocalDateTime lastCommitAt;

    @Column(name = "total_commits")
    private Integer totalCommits;

    @Column(name = "churn_factor")
    private Double churnFactor;

    @Column(name = "calculated_pers_factor")
    private Double calculatedPersFactor;

    @Column(name = "manual_pers_factor")
    private Double manualPersFactor;

    public Double getEffectivePersFactor() {
        return manualPersFactor != null ? manualPersFactor : calculatedPersFactor;
    }
}
