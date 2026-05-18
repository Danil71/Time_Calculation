package com.time.timecalc.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.time.timecalc.dto.LanguageRequest;
import com.time.timecalc.model.ProgrammingLanguage;
import com.time.timecalc.repository.ProgrammingLanguageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final ProgrammingLanguageRepository languageRepository;

    public List<ProgrammingLanguage> getAll() {
        return languageRepository.findAll();
    }

    @Transactional
    public ProgrammingLanguage create(LanguageRequest request) {
        String name = normalizeName(request.getName());
        validateLocPerFp(request.getLocPerFp());

        if (languageRepository.existsById(name)) {
            throw new RuntimeException("Язык «" + name + "» уже существует");
        }

        return languageRepository.save(ProgrammingLanguage.builder()
                .name(name)
                .locPerFp(request.getLocPerFp())
                .build());
    }

    @Transactional
    public ProgrammingLanguage update(String currentName, LanguageRequest request) {
        ProgrammingLanguage existing = languageRepository.findById(currentName)
                .orElseThrow(() -> new RuntimeException("Язык не найден"));

        validateLocPerFp(request.getLocPerFp());

        String newName = request.getName() != null && !request.getName().isBlank()
                ? normalizeName(request.getName())
                : existing.getName();

        if (!newName.equals(existing.getName()) && languageRepository.existsById(newName)) {
            throw new RuntimeException("Язык «" + newName + "» уже существует");
        }

        if (!newName.equals(existing.getName())) {
            languageRepository.delete(existing);
            return languageRepository.save(ProgrammingLanguage.builder()
                    .name(newName)
                    .locPerFp(request.getLocPerFp())
                    .build());
        }

        existing.setLocPerFp(request.getLocPerFp());
        return languageRepository.save(existing);
    }

    @Transactional
    public void delete(String name) {
        if (!languageRepository.existsById(name)) {
            throw new RuntimeException("Язык не найден");
        }
        languageRepository.deleteById(name);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Название языка обязательно");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            throw new RuntimeException("Название языка не должно превышать 50 символов");
        }
        return trimmed;
    }

    private void validateLocPerFp(Integer locPerFp) {
        if (locPerFp == null || locPerFp <= 0) {
            throw new RuntimeException("LOC/FP должен быть положительным числом");
        }
    }
}
