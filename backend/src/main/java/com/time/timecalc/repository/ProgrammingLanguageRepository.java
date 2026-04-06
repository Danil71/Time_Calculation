package com.time.timecalc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.time.timecalc.model.ProgrammingLanguage;

public interface ProgrammingLanguageRepository extends JpaRepository<ProgrammingLanguage, String> {
    
}
