package com.time.timecalc.dto;

import com.time.timecalc.model.enums.Role;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String fullName;
    private Role role;
}
