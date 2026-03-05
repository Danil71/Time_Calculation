package com.time.timecalc.dto;

import lombok.Data;

@Data
public class ProjectRequest {
    private String name;
    private String repoUrl;
    private String branchName;
    private String token; // Токен от Git в открытом виде (сервер сам его зашифрует)
}
