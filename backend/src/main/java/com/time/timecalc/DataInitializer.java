package com.time.timecalc;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.time.timecalc.model.ProgrammingLanguage;
import com.time.timecalc.model.User;
import com.time.timecalc.model.enums.Role;
import com.time.timecalc.repository.ProgrammingLanguageRepository;
import com.time.timecalc.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Инжектим шифровальщик паролей
    private final ProgrammingLanguageRepository languageRepository;

    @Override
    public void run(String... args) throws Exception {
        
        // Проверяем, есть ли уже админ в базе, чтобы не создавать его при каждом перезапуске
        if (!userRepository.existsByUsername("admin")) {
            
            User admin = User.builder()
                    .username("admin")
                    // Шифруем пароль "secret" перед сохранением в БД
                    .passwordHash(passwordEncoder.encode("secret")) 
                    .role(Role.ADMIN)
                    .fullName("Системный Администратор")
                    .build();
                    
            userRepository.save(admin);
            
            System.out.println("=======================================================");
            System.out.println("ТЕСТОВЫЙ АДМИНИСТРАТОР УСПЕШНО СОЗДАН!");
            System.out.println("Логин: admin");
            System.out.println("Пароль: secret");
            System.out.println("=======================================================");
        }

        if (languageRepository.count() == 0) {
            List<ProgrammingLanguage> languages = List.of(
                    new ProgrammingLanguage("Java", 53),
                    new ProgrammingLanguage("C++", 53),
                    new ProgrammingLanguage("C#", 54),
                    new ProgrammingLanguage("C", 128),
                    new ProgrammingLanguage("Python", 30),
                    new ProgrammingLanguage("JavaScript", 47),
                    new ProgrammingLanguage("TypeScript", 47),
                    new ProgrammingLanguage("Ruby", 46),
                    new ProgrammingLanguage("HTML", 15),
                    new ProgrammingLanguage("SQL", 13),
                    new ProgrammingLanguage("PHP", 33)
            );
            languageRepository.saveAll(languages);
            System.out.println("--- Справочник языков программирования загружен ---");
        }
    }
}