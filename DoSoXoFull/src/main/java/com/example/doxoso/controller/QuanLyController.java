package com.example.doxoso.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/quanly")
public class QuanLyController {
    @GetMapping("/report")
    public String report() {
        return "📋 ADMIN QUẢN LÝ - xem báo cáo & quản lý user";
    }
}
