package com.example.doxoso.dto;

// dto/RegisterRequest.java

import com.example.doxoso.model.RoleEnum;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private RoleEnum role;
}
