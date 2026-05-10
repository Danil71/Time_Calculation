package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodeAnalyzerTest {

    @TempDir
    File tempDir;

    @Test
    void analyzeDirectory_countsSloc_ignoresComments_andBuildsTechStack() throws Exception {
        Files.writeString(tempDir.toPath().resolve("A.java"), String.join("\n",
                "/* block comment start",
                " still comment */",
                "public class A {",
                "  // one line comment",
                "  public void m() {",
                "    if (true) {",
                "      int x = 1;",
                "    }",
                "  }",
                "}"
        ));

        Files.writeString(tempDir.toPath().resolve("b.py"), String.join("\n",
                "# comment",
                "def f():",
                "    if True:",
                "        return 1"
        ));

        CodeAnalyzer analyzer = new CodeAnalyzer();
        CodeAnalyzer.AnalysisResult res = analyzer.analyzeDirectory(tempDir);

        assertThat(res.totalSloc()).isGreaterThan(0);
        assertThat(res.techStack()).containsKeys("Java", "Python");
        assertThat(res.techStack().get("Java")).isGreaterThan(0);
        assertThat(res.techStack().get("Python")).isGreaterThan(0);
        assertThat(res.avgComplexity()).isGreaterThanOrEqualTo(1.0);
    }
}

