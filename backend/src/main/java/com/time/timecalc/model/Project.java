package com.time.timecalc.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    
    @Column(name = "repo_url")
    private String repoUrl;
    
    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "cost_per_hour")
    private BigDecimal costPerHour;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}