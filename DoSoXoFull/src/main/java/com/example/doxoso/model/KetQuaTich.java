package com.example.doxoso.model;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ket_qua_tich",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id","ngay","mien_code"})
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KetQuaTich {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="player_id", nullable=false)
    private Long playerId;

    @Column(name="player_name")
    private String playerName;

    @Column(name="ngay", nullable=false)
    private LocalDate ngay;

    @Column(name="mien_code", length=2, nullable=false) // MB | MT | MN
    private String mienCode;

    @Column(name="mien_display", length=20, nullable=false) // "MIỀN BẮC" | ...
    private String mienDisplay;

    // ---- Số liệu lưu theo yêu cầu ----
    @Column(name="tien_trung", precision=18, scale=2)
    private BigDecimal tienTrung;

    @Column(name="tien_hoa_hong", precision=18, scale=2)
    private BigDecimal tienHoaHong;

    @Column(name="tien_lon_nho", precision=18, scale=2)
    private BigDecimal tienLonNho;

    @Column(name="tien_an_thua", precision=18, scale=2)
    private BigDecimal tienAnThua; // = Trúng - (HH + Lớn/Nhỏ)

    @Column(name="tien_danh", precision=18, scale=2)
    private BigDecimal tienDanh;

    @Column(name="tien_danh_da_nhan_hoa_hong", precision=18, scale=2)
    private BigDecimal tienDanhDaNhanHoaHong;

    @Column(name="tien_danh_da_nhan_hoa_hong_cong_lon_nho", precision=18, scale=2)
    private BigDecimal tienDanhDaNhanHoaHongCongLonNho;

    // ---- Hệ thống / phiên bản ----
    @Version
    @Column(name="version")
    private Long version;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }
    @PreUpdate void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

