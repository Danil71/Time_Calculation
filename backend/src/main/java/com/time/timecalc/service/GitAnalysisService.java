package com.time.timecalc.service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
    
    private final JGitService jGitService;

    @Transactional
    public void analyzeProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        File tempDir = null;
        try {
            // 1. Клонируем репозиторий с помощью реального JGitService
            System.out.println("Начинаем клонирование: " + project.getRepoUrl());
            tempDir = jGitService.cloneRepository(project.getRepoUrl(), project.getAuthTokenEnc());

            // 2. Извлекаем историю коммитов
            System.out.println("Анализ истории коммитов...");
            List<JGitService.CommitData> commits = jGitService.getCommitHistory(tempDir);

            // 3. Сохраняем данные в БД (Участники и Коммиты)
            for (JGitService.CommitData data : commits) {
                
                // Ищем разработчика по Email (или создаем нового)
                Contributor contributor = contributorRepository
                        .findByProjectIdAndPrimaryEmail(project.getId(), data.authorEmail())
                        .orElseGet(() -> {
                            Contributor newContrib = Contributor.builder()
                                    .project(project)
                                    .primaryEmail(data.authorEmail())
                                    .displayName(data.authorName())
                                    .totalCommits(0)
                                    .build();
                            return contributorRepository.save(newContrib);
                        });

                // Обновляем статистику пользователя
                contributor.setTotalCommits(contributor.getTotalCommits() + 1);
                contributorRepository.save(contributor);

                // Преобразуем Unix-время (секунды) в LocalDateTime
                LocalDateTime commitDate = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(data.dateSeconds()), ZoneId.systemDefault()
                );
                
                // Создаем коммит
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

                // Сохраняем коммит (если он уже есть, Spring Data просто обновит его или проигнорирует, если hash совпадает)
                if (!commitRepository.existsById(data.hash())) {
                    commitRepository.save(gitCommit);
                }
            }

            System.out.println("История сохранена. Всего коммитов обработано: " + commits.size());

            System.out.println("Статический анализ файлов кода...");
            CodeAnalyzer.AnalysisResult analysisResult = codeAnalyzer.analyzeDirectory(tempDir);

            CodeSnapshot snapshot = CodeSnapshot.builder()
                    .project(project)
                    .commitHash(commits.isEmpty() ? null : commits.get(0).hash())
                    .totalSloc(analysisResult.totalSloc())
                    .avgComplexity(analysisResult.avgComplexity())
                    .churnRate(0.15) // Churn мы посчитаем в следующей задаче
                    .techStack(analysisResult.techStack())
                    .build();

            snapshotRepository.save(snapshot);

        } catch (Exception e) {
            System.err.println("Ошибка анализа Git: " + e.getMessage());
            throw new RuntimeException("Ошибка при анализе Git: " + e.getMessage());
        } finally {
            // 5. ОЧИСТКА: Обязательно удаляем папку с исходным кодом с диска сервера!
            if (tempDir != null && tempDir.exists()) {
                FileSystemUtils.deleteRecursively(tempDir);
                System.out.println("Временная папка удалена: " + tempDir.getAbsolutePath());
            }
        }
    }
}
