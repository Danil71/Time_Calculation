package com.time.timecalc.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.time.timecalc.model.ProgrammingLanguage;
import com.time.timecalc.repository.ProgrammingLanguageRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LanguageController {

    private final ProgrammingLanguageRepository languageRepository;

    @GetMapping
    public ResponseEntity<List<ProgrammingLanguage>> getAllLanguages() {
        return ResponseEntity.ok(languageRepository.findAll());
    }
}
