package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.GitCommit;
import com.time.timecalc.model.Project;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.repository.GitCommitRepository;

@ExtendWith(MockitoExtension.class)
class TeamProfilerTest {

    @Mock
    ContributorRepository contributorRepository;

    @Mock
    GitCommitRepository gitCommitRepository;

    @InjectMocks
    TeamProfiler profiler;

    @Test
    void profileContributor_setsDefaultPersFactor_whenNoCommits() {
        Contributor c = Contributor.builder()
                .id(UUID.randomUUID())
                .project(Project.builder().id(UUID.randomUUID()).name("p").repoUrl("u").build())
                .primaryEmail("a@x")
                .build();

        profiler.profileContributor(c, List.of());

        assertThat(c.getCalculatedPersFactor()).isEqualTo(1.0);
    }

    @Test
    void profileContributor_computesChurnAndClampsPersFactor() {
        LocalDateTime first = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime last = LocalDateTime.of(2026, 1, 15, 0, 0);

        Contributor c = Contributor.builder()
                .id(UUID.randomUUID())
                .project(Project.builder().id(UUID.randomUUID()).name("p").repoUrl("u").build())
                .primaryEmail("a@x")
                .firstCommitAt(first)
                .lastCommitAt(last)
                .build();

        // churn high: deleted/(added+deleted) = 80/(20+80)=0.8 -> +0.15
        // experience < 2 months -> +0.10
        GitCommit commit = GitCommit.builder()
                .hash("h1")
                .commitDate(LocalDateTime.now())
                .message("x")
                .linesAdded(20)
                .linesDeleted(80)
                .filesChanged(1)
                .isMerge(false)
                .build();

        profiler.profileContributor(c, List.of(commit));

        assertThat(c.getChurnFactor()).isCloseTo(0.8, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(c.getCalculatedPersFactor()).isCloseTo(1.25, org.assertj.core.data.Offset.offset(1e-12));
    }
}

