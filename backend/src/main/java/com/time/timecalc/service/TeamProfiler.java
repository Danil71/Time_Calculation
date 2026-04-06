package com.time.timecalc.service;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.time.timecalc.model.Contributor;
import com.time.timecalc.model.GitCommit;

@Service
public class TeamProfiler {

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
}
