package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.time.timecalc.dto.AuthRequest;
import com.time.timecalc.dto.AuthResponse;
import com.time.timecalc.dto.RegisterRequest;
import com.time.timecalc.model.User;
import com.time.timecalc.model.enums.Role;
import com.time.timecalc.repository.UserRepository;
import com.time.timecalc.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    AuthenticationManager authenticationManager;

    @InjectMocks
    AuthService authService;

    @Test
    void register_throwsIfUsernameExists() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("u");
        req.setPassword("p");
        req.setFullName("F");
        req.setRole(Role.DEVELOPER);

        when(userRepository.existsByUsername("u")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void register_encodesPassword_savesUser_returnsJwt() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("u");
        req.setPassword("p");
        req.setFullName("F");
        req.setRole(Role.ADMIN);

        when(userRepository.existsByUsername("u")).thenReturn(false);
        when(passwordEncoder.encode("p")).thenReturn("HASH");
        when(jwtService.generateToken(any(User.class))).thenReturn("JWT");

        AuthResponse res = authService.register(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("u");
        assertThat(saved.getPasswordHash()).isEqualTo("HASH");
        assertThat(saved.getFullName()).isEqualTo("F");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);

        assertThat(res.getToken()).isEqualTo("JWT");
        assertThat(res.getUsername()).isEqualTo("u");
        assertThat(res.getFullName()).isEqualTo("F");
        assertThat(res.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void login_authenticates_loadsUser_andReturnsJwt() {
        AuthRequest req = new AuthRequest();
        req.setUsername("u");
        req.setPassword("p");

        User user = User.builder()
                .username("u")
                .passwordHash("HASH")
                .fullName("F")
                .role(Role.DEVELOPER)
                .build();

        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("JWT");

        AuthResponse res = authService.login(req);

        verify(authenticationManager).authenticate(eq(new UsernamePasswordAuthenticationToken("u", "p")));
        assertThat(res.getToken()).isEqualTo("JWT");
        assertThat(res.getUsername()).isEqualTo("u");
        assertThat(res.getFullName()).isEqualTo("F");
        assertThat(res.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    void login_throwsIfAuthenticationFails() {
        AuthRequest req = new AuthRequest();
        req.setUsername("u");
        req.setPassword("bad");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}

