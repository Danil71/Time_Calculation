package com.time.timecalc.service;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JGitService {

    /**
     * Дополнительные хосты Git (через запятую), для которых нужна схема GitLab HTTPS:
     * username {@code oauth2}, пароль — PAT. Укажите свой корпоративный хост, если в URL нет подстроки "gitlab".
     */
    private final String gitlabOauth2ExtraHosts;
    private final String gitlabUsername;

    public JGitService(@Value("${timecalc.git.gitlab-oauth2-hosts:}") String gitlabOauth2ExtraHosts,
                        @Value("${timecalc.git.gitlab-username:oauth2}") String gitlabUsername ) {
        this.gitlabOauth2ExtraHosts = gitlabOauth2ExtraHosts == null ? "" : gitlabOauth2ExtraHosts;
        this.gitlabUsername = gitlabUsername;
    }

    public record CommitData(
            String hash, String authorEmail, String authorName,
            long dateSeconds, String message,
            int linesAdded, int linesDeleted, int filesChanged
    ) {}

    public File cloneRepository(String repoUrl, String token, String branchName) throws Exception {

        File tempDir = Files.createTempDirectory("cocomo_git_").toFile();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(tempDir)
                .setCloneAllBranches(false);

        if (branchName != null && !branchName.isEmpty()) {
            cloneCommand.setBranch(branchName);

            cloneCommand.setBranchesToClone(java.util.Collections.singletonList("refs/heads/" + branchName));
        }
                
        if (token != null && !token.isEmpty()) {
            cloneCommand.setCredentialsProvider(credentialsProviderForRepo(repoUrl, token));
        }

        try (Git git = cloneCommand.call()) {
            System.out.println("Репозиторий успешно склонирован (ветка " + branchName + ") в: " + tempDir.getAbsolutePath());
        }

        return tempDir;
    }

    public List<CommitData> getCommitHistory(File repoDir) throws Exception {
        List<CommitData> history = new ArrayList<>();

        try (Git git = Git.open(repoDir);
             Repository repository = git.getRepository();
             RevWalk revWalk = new RevWalk(repository);
             DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            df.setRepository(repository);
            df.setDetectRenames(true);

            Iterable<RevCommit> commits = git.log().call();

            for (RevCommit commit : commits) {
                int linesAdded = 0;
                int linesDeleted = 0;
                int filesChanged = 0;

                AbstractTreeIterator currentTreeParser = new CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree().getId());
                AbstractTreeIterator parentTreeParser;

                if (commit.getParentCount() > 0) {
                    RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                    parentTreeParser = new CanonicalTreeParser(null, repository.newObjectReader(), parent.getTree().getId());
                } else {
                    parentTreeParser = new EmptyTreeIterator();
                }

                List<DiffEntry> diffs = df.scan(parentTreeParser, currentTreeParser);
                filesChanged = diffs.size();

                for (DiffEntry diff : diffs) {
                    for (Edit edit : df.toFileHeader(diff).toEditList()) {
                        linesDeleted += edit.getEndA() - edit.getBeginA();
                        linesAdded += edit.getEndB() - edit.getBeginB();
                    }
                }

                history.add(new CommitData(
                        commit.getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        commit.getAuthorIdent().getName(),
                        commit.getCommitTime(),
                        commit.getFullMessage(),
                        linesAdded,
                        linesDeleted,
                        filesChanged
                ));
            }
        }
        return history;
    }

    /**
     * GitLab (в т.ч. self-hosted) для HTTPS с PAT ожидает {@code oauth2} как имя пользователя.
     * GitHub и ряд других систем принимают токен как username и пустой пароль.
     */
    private UsernamePasswordCredentialsProvider credentialsProviderForRepo(String repoUrl, String token) {
        if (useGitlabOauth2Style(repoUrl)) {
            System.out.println("Используется GitLab OAuth2 стиль");
            return new UsernamePasswordCredentialsProvider(gitlabUsername, token);
        }
        System.out.println("Используется GitHub стиль");
        return new UsernamePasswordCredentialsProvider(token, "");
    }

    private boolean useGitlabOauth2Style(String repoUrl) {
        if (repoUrl == null) {
            return false;
        }
        if (repoUrl.toLowerCase().contains("gitlab")) {
            return true;
        }
        try {
            URI uri = new URI(repoUrl);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            host = host.toLowerCase();
            for (String part : gitlabOauth2ExtraHosts.split(",")) {
                String h = part.trim().toLowerCase();
                if (!h.isEmpty() && host.equals(h)) {
                    return true;
                }
            }
        } catch (URISyntaxException ignored) {
            // не GitLab-схема по URL
        }
        return false;
    }
}
