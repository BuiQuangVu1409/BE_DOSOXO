package com.example.doxoso.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tong")
public class TongController {
    @GetMapping("/dashboard")
    public String dashboard() {
        return "📊 ADMIN TỔNG - toàn quyền hệ thống";
    }
}
