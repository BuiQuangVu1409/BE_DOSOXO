package com.example.doxoso.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ket_qua_tich",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "ngay", "mien_code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KetQuaTich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "ngay", nullable = false)
    private LocalDate ngay;

    @Column(name = "mien_code", length = 2, nullable = false) // MB | MT | MN
    private String mienCode;

    @Column(name = "mien_display", length = 20, nullable = false) // "MIỀN BẮC" | ...
    private String mienDisplay;

    // ---- Số liệu chính (lưu DB) ----

    // Tiền trúng kèo THƯỜNG (KHÔNG gồm LỚN/NHỎ)
    @Column(name = "tien_trung", precision = 18, scale = 2)
    private BigDecimal tienTrung;

    // Tổng tiền hoa hồng (tính trên tiền đánh thường)
    @Column(name = "tien_hoa_hong", precision = 18, scale = 2)
    private BigDecimal tienHoaHong;

    // NET LỚN/NHỎ TỔNG = tienLonNet + tienNhoNet (dương/âm)
    @Column(name = "tien_lon_nho", precision = 18, scale = 2)
    private BigDecimal tienLonNho;

    // Tiền ăn/thua kèo THƯỜNG = tienTrung - tienHoaHong
    @Column(name = "tien_an_thua", precision = 18, scale = 2)
    private BigDecimal tienAnThua;

    // Tổng tiền đánh theo miền (KHÔNG LỚN/NHỎ, đã nhân số đài)
    @Column(name = "tien_danh", precision = 18, scale = 2)
    private BigDecimal tienDanh;

    @Column(name = "tien_danh_da_nhan_hoa_hong", precision = 18, scale = 2)
    private BigDecimal tienDanhDaNhanHoaHong;

    // Thông tin thêm: hoa hồng + NET LỚN/NHỎ
    @Column(name = "tien_danh_da_nhan_hoa_hong_cong_lon_nho", precision = 18, scale = 2)
    private BigDecimal tienDanhDaNhanHoaHongCongLonNho;

    // ---- Chi tiết trúng (lưu JSON text) ----
    @Column(name = "chi_tiet_trung", columnDefinition = "TEXT")
    private String chiTietTrung;  // JSON string cho FE parse

    // ---- Field chỉ dùng hiển thị, không lưu DB ----

    // Tổng tiền ĐÁNH LỚN / ĐÁNH NHỎ theo miền
    @Transient
    private BigDecimal tienLonDanh;   // stake LỚN

    @Transient
    private BigDecimal tienNhoDanh;   // stake NHỎ

    // NET riêng LỚN / NHỎ (trúng - đánh, đã cộng hết kèo)
    @Transient
    private BigDecimal tienLonNet;    // net LỚN

    @Transient
    private BigDecimal tienNhoNet;    // net NHỎ

    // % hoa hồng của player (lấy từ bảng players)
    @Transient
    private Double hoaHongPlayer;

    // ---- Version & timestamps ----
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
