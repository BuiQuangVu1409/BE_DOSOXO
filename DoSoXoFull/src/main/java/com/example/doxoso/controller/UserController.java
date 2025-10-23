package com.example.doxoso.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/profile")
    public String profile() {
        return "👤 USER - xem thông tin cá nhân";
    }
}
