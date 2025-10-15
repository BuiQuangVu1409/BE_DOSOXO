package com.example.doxoso.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LichQuayXoSo {
        private LocalDate ngay;
        private String thu;
        // key: "MIỀN BẮC" | "MIỀN TRUNG" | "MIỀN NAM"; value: danh sách đài trong ngày
        private Map<String, List<String>> ketQua;
}
