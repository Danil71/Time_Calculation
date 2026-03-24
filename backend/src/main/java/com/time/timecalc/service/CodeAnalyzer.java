package com.time.timecalc.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public class CodeAnalyzer {

    // Класс для возврата двух метрик сразу
    public record AnalysisResult(long totalSloc, double avgComplexity, Map<String, Long> techStack) {}

    // Список поддерживаемых расширений и их языков
    private static final Map<String, String> SUPPORTED_EXTENSIONS = Map.of(
            ".java", "Java",
            ".py", "Python",
            ".js", "JavaScript",
            ".ts", "TypeScript",
            ".cpp", "C++",
            ".cs", "C#"
    );

    public AnalysisResult analyzeDirectory(File repoDir) {
        long totalSloc = 0;
        long totalComplexity = 0;
        int filesCount = 0;
        Map<String, Long> techStack = new HashMap<>();

        try (Stream<Path> paths = Files.walk(repoDir.toPath())) {
            List<Path> filesToAnalyze = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .toList();

            for (Path path : filesToAnalyze) {
                String language = getLanguage(path);
                List<String> lines = Files.readAllLines(path);

                long fileSloc = countLogicalLines(lines, language);
                long fileComplexity = calculateMcCabeComplexity(lines);

                totalSloc += fileSloc;
                totalComplexity += fileComplexity;
                filesCount++;

                // Добавляем строки в статистику по языкам
                techStack.put(language, techStack.getOrDefault(language, 0L) + fileSloc);
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении файлов репозитория: " + e.getMessage());
        }

        double avgComplexity = filesCount > 0 ? (double) totalComplexity / filesCount : 1.0;
        if (totalSloc == 0) totalSloc = 1; // Защита от пустых репо

        return new AnalysisResult(totalSloc, avgComplexity, techStack);
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString();
        return SUPPORTED_EXTENSIONS.keySet().stream().anyMatch(fileName::endsWith);
    }

    private String getLanguage(Path path) {
        String fileName = path.getFileName().toString();
        for (Map.Entry<String, String> entry : SUPPORTED_EXTENSIONS.entrySet()) {
            if (fileName.endsWith(entry.getKey())) return entry.getValue();
        }
        return "Unknown";
    }

    // Подсчет логических строк (SLOC) - исключаем пустые строки и однострочные комментарии
    private long countLogicalLines(List<String> lines, String language) {
        long sloc = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Упрощенная логика игнорирования комментариев для C-подобных языков и Java
            if (language.equals("Python")) {
                if (trimmed.startsWith("#")) continue;
            } else {
                if (trimmed.startsWith("/*")) inBlockComment = true;
                if (inBlockComment) {
                    if (trimmed.contains("*/")) inBlockComment = false;
                    continue;
                }
                if (trimmed.startsWith("//")) continue;
            }
            sloc++;
        }
        return sloc;
    }

    // Упрощенный расчет сложности МакКейба v(G) = E - N + 2
    // На практике v(G) вычисляется как 1 + количество точек ветвления (if, for, while, case, &&, ||)
    private long calculateMcCabeComplexity(List<String> lines) {
        long complexity = 1; // Базовая сложность функции/файла = 1
        for (String line : lines) {
            String l = line.trim();
            // Ищем ключевые слова ветвления алгоритма
            if (l.startsWith("if ") || l.startsWith("if(") ||
                l.startsWith("for ") || l.startsWith("for(") ||
                l.startsWith("while ") || l.startsWith("while(") ||
                l.startsWith("case ") || l.contains("catch ") ||
                l.contains("&&") || l.contains("||") || l.contains("?")) {
                complexity++;
            }
        }
        return complexity;
    }
}
