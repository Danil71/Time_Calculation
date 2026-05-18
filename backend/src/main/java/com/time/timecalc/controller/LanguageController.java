package com.time.timecalc.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.dto.LanguageRequest;
import com.time.timecalc.model.ProgrammingLanguage;
import com.time.timecalc.service.LanguageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    public ResponseEntity<List<ProgrammingLanguage>> getAllLanguages() {
        return ResponseEntity.ok(languageService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ProgrammingLanguage> createLanguage(@RequestBody LanguageRequest request) {
        return ResponseEntity.ok(languageService.create(request));
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ProgrammingLanguage> updateLanguage(
            @PathVariable String name,
            @RequestBody LanguageRequest request) {
        return ResponseEntity.ok(languageService.update(name, request));
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> deleteLanguage(@PathVariable String name) {
        languageService.delete(name);
        return ResponseEntity.noContent().build();
    }
}
