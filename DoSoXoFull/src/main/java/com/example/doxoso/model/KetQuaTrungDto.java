package com.example.doxoso.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KetQuaTrungDto {
    private String tenDai;          // Đài trúng (ví dụ: Đài Cà Mau)
    private String giai;            // Giải trúng (ví dụ: G6, ĐẶC BIỆT)
    private String loaiTrung;       // Bao lô, Lô thượng, Đặc biệt
    private double tienTrung;       // Tổng tiền trúng lần này
    private double tienBaoLo;       // Tiền trúng từ bao lô
    private double tienThuong;      // Tiền trúng từ thưởng
    private double tienDacBiet;     // Tiền trúng từ đặc biệt
    private String soTrung;         // Số trúng (ví dụ: 45123)
}
