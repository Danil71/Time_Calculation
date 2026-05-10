package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.Project;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.repository.GitCommitRepository;
import com.time.timecalc.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class GitAnalysisServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    GitCommitRepository commitRepository;

    @Mock
    ContributorRepository contributorRepository;

    @Mock
    CodeSnapshotRepository snapshotRepository;

    @Mock
    CodeAnalyzer codeAnalyzer;

    @Mock
    TeamProfiler teamProfiler;

    @Mock
    JGitService jGitService;

    @InjectMocks
    GitAnalysisService service;

    @Test
    void analyzeProject_throwsIfProjectNotFound() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyzeProject(projectId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Проект не найден");
    }

    @Test
    void analyzeProject_savesCommitsContributorsSnapshot_andComputesChurnIgnoringMergeCommits() throws Exception {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .id(projectId)
                .name("p")
                .repoUrl("url")
                .branchName("main")
                .authTokenEnc("t")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        File tempDir = Files.createTempDirectory("gitAnalysisTest_").toFile();
        when(jGitService.cloneRepository("url", "t", "main")).thenReturn(tempDir);

        List<JGitService.CommitData> commits = List.of(
                new JGitService.CommitData("h1", "a@x", "A", 1700000000L, "feat: add", 10, 5, 2),
                new JGitService.CommitData("h2", "a@x", "A", 1700001000L, "Merge branch 'x'", 100, 50, 10)
        );
        when(jGitService.getCommitHistory(tempDir)).thenReturn(commits);

        Contributor contributor = Contributor.builder()
                .id(UUID.randomUUID())
                .project(project)
                .primaryEmail("a@x")
                .displayName("A")
                .totalCommits(0)
                .aliases(List.of())
                .firstCommitAt(LocalDateTime.now())
                .lastCommitAt(LocalDateTime.now())
                .build();

        AtomicInteger lookupCalls = new AtomicInteger();
        when(contributorRepository.findByProjectIdAndPrimaryEmail(projectId, "a@x"))
                .thenAnswer(inv -> lookupCalls.getAndIncrement() == 0 ? Optional.empty() : Optional.of(contributor));
        when(contributorRepository.save(any(Contributor.class))).thenReturn(contributor);

        when(commitRepository.existsById("h1")).thenReturn(false);
        when(commitRepository.existsById("h2")).thenReturn(false);

        when(codeAnalyzer.analyzeDirectory(tempDir))
                .thenReturn(new CodeAnalyzer.AnalysisResult(1234L, 2.5, Map.of("Java", 1234L)));

        service.analyzeProject(projectId);

        // churn: counts only non-merge lines -> deleted/(added+deleted)=5/15
        ArgumentCaptor<CodeSnapshot> snapshotCaptor = ArgumentCaptor.forClass(CodeSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        CodeSnapshot savedSnapshot = snapshotCaptor.getValue();
        assertThat(savedSnapshot.getProject()).isEqualTo(project);
        assertThat(savedSnapshot.getTotalSloc()).isEqualTo(1234L);
        assertThat(savedSnapshot.getAvgComplexity()).isEqualTo(2.5);
        assertThat(savedSnapshot.getChurnRate()).isCloseTo(5.0 / 15.0, within(1e-12));
        assertThat(savedSnapshot.getTechStack()).containsEntry("Java", 1234L);

        verify(teamProfiler).profileContributor(eq(contributor), any());
    }
}

