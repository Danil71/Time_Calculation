package com.time.timecalc.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ml_calibration_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlCalibrationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "performed_at", updatable = false)
    private LocalDateTime performedAt;

    @Column(name = "rmse_score")
    private Double rmseScore;

    @Column(name = "old_a")
    private Double oldA;

    @Column(name = "old_b")
    private Double oldB;

    @Column(name = "new_a")
    private Double newA;

    @Column(name = "new_b")
    private Double newB;

    @Column(name = "projects_used_count")
    private Integer projectsUsedCount;
}
