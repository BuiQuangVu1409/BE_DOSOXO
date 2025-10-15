package com.example.doxoso.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTongTienDanhTheoMienDto {
    private Long playerId;
    private String playerName;       // nếu không có trong SoNguoiChoi thì để null
    private BigDecimal mienBac;      // tổng tiền đánh miền Bắc
    private BigDecimal mienTrung;    // tổng tiền đánh miền Trung
    private BigDecimal mienNam;      // tổng tiền đánh miền Nam
    private BigDecimal tong;         // tổng toàn bộ
    private LocalDate ngay;
}

