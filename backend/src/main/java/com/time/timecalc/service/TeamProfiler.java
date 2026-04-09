package com.time.timecalc.service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.GitCommit;
import com.time.timecalc.repository.ContributorRepository;
import com.time.timecalc.repository.GitCommitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamProfiler {

    private final ContributorRepository contributorRepository;
    private final GitCommitRepository gitCommitRepository;

    /**
     * Анализирует историю коммитов участника и обновляет его рейтинг (PERS Factor)
     */
    public void profileContributor(Contributor contributor, List<GitCommit> commits) {
        if (commits == null || commits.isEmpty()) {
            contributor.setCalculatedPersFactor(1.0);
            return;
        }

        int totalAdded = 0;
        int totalDeleted = 0;

        for (GitCommit commit : commits) {
            if (commit.getLinesAdded() != null) totalAdded += commit.getLinesAdded();
            if (commit.getLinesDeleted() != null) totalDeleted += commit.getLinesDeleted();
        }

        double churn = 0.0;
        int totalChanged = totalAdded + totalDeleted;
        if (totalChanged > 0) {
            churn = (double) totalDeleted / totalChanged;
        }
        contributor.setChurnFactor(churn);

        long monthsExperience = 0;
        if (contributor.getFirstCommitAt() != null && contributor.getLastCommitAt() != null) {
            monthsExperience = ChronoUnit.MONTHS.between(
                    contributor.getFirstCommitAt(), 
                    contributor.getLastCommitAt()
            );
        }

        double persRating = 1.0; 

        if (churn > 0.4) persRating += 0.15;
        else if (churn < 0.15) persRating -= 0.10;

        if (monthsExperience > 12) persRating -= 0.15;
        else if (monthsExperience < 2) persRating += 0.10;

        if (persRating < 0.7) persRating = 0.7;
        if (persRating > 1.4) persRating = 1.4;

        contributor.setCalculatedPersFactor(persRating);
    }

    @Transactional
    public void mergeAliases(UUID primaryId, UUID duplicateId) {
        Contributor primary = contributorRepository.findById(primaryId)
                .orElseThrow(() -> new RuntimeException("Основной профиль не найден"));
        
        Contributor duplicate = contributorRepository.findById(duplicateId)
                .orElseThrow(() -> new RuntimeException("Дубликат не найден"));

        // 0. Переназначаем все коммиты с дубликата на основной профиль,
        // иначе удаление duplicate упадет на FK git_commits(contributor_id) -> contributors(id)
        gitCommitRepository.reassignContributor(primary, duplicateId);

        // 1. Добавляем email дубликата в алиасы
        List<String> aliases = primary.getAliases();
        if (aliases == null) aliases = new ArrayList<>();
        
        if (!aliases.contains(duplicate.getPrimaryEmail())) {
            aliases.add(duplicate.getPrimaryEmail());
        }
        primary.setAliases(aliases);

        // 2. Пересчитываем статистику
        int primaryCommits = primary.getTotalCommits() != null ? primary.getTotalCommits() : 0;
        int duplicateCommits = duplicate.getTotalCommits() != null ? duplicate.getTotalCommits() : 0;
        primary.setTotalCommits(primaryCommits + duplicateCommits);
        if (duplicate.getFirstCommitAt() != null && 
           (primary.getFirstCommitAt() == null || duplicate.getFirstCommitAt().isBefore(primary.getFirstCommitAt()))) {
            primary.setFirstCommitAt(duplicate.getFirstCommitAt());
        }

        // 3. Удаляем дубликат из базы
        contributorRepository.delete(duplicate);
        
        // 4. Сохраняем результат
        contributorRepository.save(primary);
    }
}
