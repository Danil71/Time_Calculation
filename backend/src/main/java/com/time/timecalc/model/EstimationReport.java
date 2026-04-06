package com.time.timecalc.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.time.timecalc.model.json.CocomoParams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estimation_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private CodeSnapshot snapshot;

    @CreationTimestamp
    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "effort_pm")
    private Double effortPm;

    @Column(name = "duration_months")
    private Double durationMonths;

    @Column(name = "team_size")
    private Double teamSize;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_params", columnDefinition = "jsonb")
    private CocomoParams appliedParams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_fp_details", columnDefinition = "jsonb")
    private Map<String, Double> targetFpDetails;
}
