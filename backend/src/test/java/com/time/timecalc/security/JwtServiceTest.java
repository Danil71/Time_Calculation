package com.time.timecalc.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.time.timecalc.model.User;
import com.time.timecalc.model.enums.Role;

class JwtServiceTest {

    @Test
    void generateToken_andExtractUsername_andValidate() {
        JwtService jwtService = new JwtService();

        User user = User.builder()
                .username("u")
                .passwordHash("HASH")
                .role(Role.DEVELOPER)
                .build();

        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();

        assertThat(jwtService.extractUsername(token)).isEqualTo("u");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();

        User other = User.builder()
                .username("other")
                .passwordHash("HASH")
                .role(Role.DEVELOPER)
                .build();
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }
}

