package com.time.timecalc.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "git_commits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitCommit {
    @Id
    @Column(length = 40)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id")
    private Contributor contributor;

    @Column(name = "commit_date", nullable = false)
    private LocalDateTime commitDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "lines_added")
    private Integer linesAdded;

    @Column(name = "lines_deleted")
    private Integer linesDeleted;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "is_merge")
    private Boolean isMerge;
}
