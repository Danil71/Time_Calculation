package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JGitServiceTest {

    @TempDir
    File tempDir;

    @Test
    void getCommitHistory_readsCommitsFromLocalRepo() throws Exception {
        // init local repo
        try (Git git = Git.init().setDirectory(tempDir).call()) {
            Files.writeString(tempDir.toPath().resolve("a.txt"), "line1\nline2\n");
            git.add().addFilepattern("a.txt").call();
            git.commit()
                    .setMessage("init")
                    .setAuthor(new PersonIdent("A", "a@x"))
                    .call();

            Files.writeString(tempDir.toPath().resolve("a.txt"), "line1\nline2\nline3\n");
            git.add().addFilepattern("a.txt").call();
            git.commit()
                    .setMessage("feat: add line")
                    .setAuthor(new PersonIdent("A", "a@x"))
                    .call();
        }

        JGitService service = new JGitService();
        List<JGitService.CommitData> history = service.getCommitHistory(tempDir);

        assertThat(history).isNotEmpty();
        assertThat(history.get(0).hash()).isNotBlank();
        assertThat(history.get(0).hash()).hasSize(40);
        assertThat(history.get(0).authorEmail()).isEqualTo("a@x");
        assertThat(history.get(0).message()).isNotBlank();
        assertThat(history.get(0).filesChanged()).isGreaterThanOrEqualTo(0);
        assertThat(history.get(0).linesAdded()).isGreaterThanOrEqualTo(0);
        assertThat(history.get(0).linesDeleted()).isGreaterThanOrEqualTo(0);
    }
}

