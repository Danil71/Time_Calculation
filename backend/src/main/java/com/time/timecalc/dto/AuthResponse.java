package com.time.timecalc.dto;

import com.time.timecalc.model.enums.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private String fullName;
    private Role role;
}
