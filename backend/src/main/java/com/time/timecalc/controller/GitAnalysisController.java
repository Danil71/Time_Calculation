package com.time.timecalc.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.service.GitAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/git")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GitAnalysisController {

    private final GitAnalysisService gitAnalysisService;

    // POST /api/git/{projectId}/analyze
    @PostMapping("/{projectId}/analyze")
    public ResponseEntity<String> runGitAnalysis(@PathVariable UUID projectId) {
        
        // В реальном Enterprise-проекте этот вызов стоит сделать асинхронным 
        // (через @Async или Kafka), так как клонирование может занять пару минут.
        // Для MVP мы оставляем синхронный вызов.
        
        gitAnalysisService.analyzeProject(projectId);
        
        return ResponseEntity.ok("Анализ репозитория успешно завершен. История коммитов сохранена.");
    }
}
