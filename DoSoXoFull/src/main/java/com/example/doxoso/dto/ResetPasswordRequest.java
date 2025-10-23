package com.example.doxoso.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;       // reset token gửi kèm link
    private String newPassword; // mật khẩu mới
}
