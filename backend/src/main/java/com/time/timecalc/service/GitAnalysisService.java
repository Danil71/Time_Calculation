package com.time.timecalc.service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import com.time.timecalc.model.CodeSnapshot;
import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.GitCommit;
import com.time.timecalc.model.Project;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.repository.GitCommitRepository;
import com.time.timecalc.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitAnalysisService {

    private final ProjectRepository projectRepository;
    private final GitCommitRepository commitRepository;
    private final ContributorRepository contributorRepository;
    private final CodeSnapshotRepository snapshotRepository;
    private final CodeAnalyzer codeAnalyzer;
    private final TeamProfiler teamProfiler;
    private final JGitService jGitService;

    @Transactional
    public void analyzeProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        File tempDir = null;
        try {
            System.out.println("Начинаем клонирование: " + project.getRepoUrl());
            tempDir = jGitService.cloneRepository(project.getRepoUrl(), project.getAuthTokenEnc(), project.getBranchName());

            System.out.println("Анализ истории коммитов...");
            List<JGitService.CommitData> commits = jGitService.getCommitHistory(tempDir);

            // Нельзя использовать Contributor как ключ HashMap: он мутируется (totalCommits, first/lastCommitAt),
            // а Lombok @Data включает эти поля в hashCode/equals -> ключ становится "плавающим".
            Map<String, Contributor> contributorsByEmail = new HashMap<>();
            Map<String, List<GitCommit>> contributorCommitsMap = new HashMap<>();

            long totalLinesAdded = 0;
            long totalLinesDeleted = 0;

            for (JGitService.CommitData data : commits) {
                
                if (!data.message().startsWith("Merge")) {
                    totalLinesAdded += data.linesAdded();
                    totalLinesDeleted += data.linesDeleted();
                }

                Contributor contributor = contributorsByEmail.get(data.authorEmail());
                if (contributor == null) {
                    contributor = contributorRepository
                            .findByProjectIdAndPrimaryEmail(project.getId(), data.authorEmail())
                            .orElseGet(() -> contributorRepository.save(
                                    Contributor.builder()
                                            .project(project)
                                            .primaryEmail(data.authorEmail())
                                            .displayName(data.authorName())
                                            .aliases(new ArrayList<>())
                                            .totalCommits(0)
                                            .build()
                            ));
                    contributorsByEmail.put(data.authorEmail(), contributor);
                }

                LocalDateTime commitDate = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(data.dateSeconds()), ZoneId.systemDefault()
                );
                
                GitCommit gitCommit = GitCommit.builder()
                        .hash(data.hash())
                        .project(project)
                        .contributor(contributor)
                        .commitDate(commitDate)
                        .message(data.message())
                        .linesAdded(data.linesAdded())
                        .linesDeleted(data.linesDeleted())
                        .filesChanged(data.filesChanged())
                        .isMerge(data.message().startsWith("Merge"))
                        .build();

                if (!commitRepository.existsById(data.hash())) {
                    commitRepository.save(gitCommit);
                    contributor.setTotalCommits(contributor.getTotalCommits() + 1);
                    
                    if (contributor.getFirstCommitAt() == null || commitDate.isBefore(contributor.getFirstCommitAt())) {
                        contributor.setFirstCommitAt(commitDate);
                    }
                    if (contributor.getLastCommitAt() == null || commitDate.isAfter(contributor.getLastCommitAt())) {
                        contributor.setLastCommitAt(commitDate);
                    }
                }
                contributorCommitsMap.computeIfAbsent(data.authorEmail(), k -> new ArrayList<>()).add(gitCommit);
            }

            long totalChanged = totalLinesAdded + totalLinesDeleted;
            double projectChurn = totalChanged == 0 ? 0 : (double) totalLinesDeleted / totalChanged;
            
            for (Map.Entry<String, List<GitCommit>> entry : contributorCommitsMap.entrySet()) {
                Contributor contributor = contributorsByEmail.get(entry.getKey());
                if (contributor == null) continue;
                teamProfiler.profileContributor(contributor, entry.getValue());
                contributorRepository.save(contributor);
            }

            System.out.println("История сохранена. Всего коммитов обработано: " + commits.size());

            System.out.println("Статический анализ файлов кода...");
            CodeAnalyzer.AnalysisResult analysisResult = codeAnalyzer.analyzeDirectory(tempDir);

            CodeSnapshot snapshot = CodeSnapshot.builder()
                    .project(project)
                    .commitHash(commits.isEmpty() ? null : commits.get(0).hash())
                    .totalSloc(analysisResult.totalSloc())
                    .avgComplexity(analysisResult.avgComplexity())
                    .churnRate(projectChurn)
                    .techStack(analysisResult.techStack())
                    .build();

            snapshotRepository.save(snapshot);

        } catch (Exception e) {
            String detail = e.getMessage();
            if (detail != null && detail.contains("no CredentialsProvider has been registered")) {
                detail = "нужен Personal Access Token (PAT) в настройках проекта. "
                        + "Вход в GitLab в браузере не подставляется в клон на сервере; для приватного инстанса токен обязателен.";
            }
            System.err.println("Ошибка анализа Git: " + e.getMessage());
            throw new RuntimeException("Ошибка при анализе Git: " + detail);
        } finally {
            if (tempDir != null && tempDir.exists()) {
                FileSystemUtils.deleteRecursively(tempDir);
                System.out.println("Временная папка удалена: " + tempDir.getAbsolutePath());
            }
        }
    }
}
