package com.example.doxoso.controller;

import com.example.doxoso.config.JwtService;
import com.example.doxoso.dto.*;
import com.example.doxoso.model.User;
import com.example.doxoso.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private UserRepository repo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    // ===== Đăng ký (giữ nguyên) =====
    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        if (repo.findByUsername(request.getUsername()).isPresent()) {
            return "Tên đăng nhập đã tồn tại!";
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        repo.save(user);
        return "✅ Đăng ký thành công với vai trò: " + request.getRole();
    }

    // ===== Đăng nhập (giữ nguyên) =====
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        return jwtService.generateToken(request.getUsername());
    }

    // ===== Kiểm tra login (giữ nguyên) =====
    @GetMapping("/check")
    public String checkLogin() {
        return "🔒 Bạn đã đăng nhập hợp lệ!";
    }

    // ===== Quên mật khẩu: sinh reset token & (tạm thời) trả ra để test =====
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest req) {
        User user = repo.findByUsername(req.getUsername())
                .orElse(null);
        if (user == null) {
            // Tránh lộ thông tin tồn tại user → trả về thông báo chung
            return "Nếu tài khoản tồn tại, liên kết đặt lại mật khẩu sẽ được gửi.";
        }
        String resetToken = jwtService.generateResetToken(user.getUsername());

        // TODO: Gửi resetToken qua email kèm link, ví dụ:
        // https://your-frontend/reset-password?token=...

        // Để bạn test Postman trước, mình trả trực tiếp token:
        return "RESET_TOKEN: " + resetToken;
    }

    // ===== Đặt lại mật khẩu bằng reset token =====
    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequest req) {
        String token = req.getToken();
        if (!jwtService.isTokenValid(token) || !jwtService.isResetToken(token)) {
            return "❌ Token không hợp lệ hoặc đã hết hạn.";
        }
        String username = jwtService.extractUsername(token);
        User user = repo.findByUsername(username).orElse(null);
        if (user == null) return "❌ Tài khoản không tồn tại.";

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        repo.save(user);
        return "✅ Đổi mật khẩu thành công! Hãy đăng nhập lại.";
    }

    // ===== (Tuỳ chọn) Đổi mật khẩu khi đã đăng nhập =====
    @PostMapping("/change-password")
    public String changePassword(@RequestBody ChangePasswordRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "❌ Chưa đăng nhập.";

        String username = auth.getName();
        User user = repo.findByUsername(username).orElse(null);
        if (user == null) return "❌ Tài khoản không tồn tại.";

        // Xác thực mật khẩu hiện tại
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(username, req.getCurrentPassword()));
        } catch (BadCredentialsException ex) {
            return "❌ Mật khẩu hiện tại không đúng.";
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        repo.save(user);
        return "✅ Đổi mật khẩu thành công!";
    }
}
