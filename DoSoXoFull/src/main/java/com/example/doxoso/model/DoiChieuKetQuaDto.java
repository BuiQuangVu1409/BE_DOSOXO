package com.example.doxoso.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
//@Table(name = "Doi Chieu Ket Qua")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoiChieuKetQuaDto {

    private String soDanh;
    private String tenDai;
    private String mien;
    private LocalDate ngay;
    private String thu;
    private String cachDanh;
    private String tienDanh;

    private boolean trung;
    private String giaiTrung;
    private String cachTrung;
    @JsonSerialize(using = DieuChinhKieuSo.class)
    private Double tienTrung;
    private String ghiChu;
    private List<String> saiLyDo;
    private String ketQua;

    @JsonSerialize(using = DieuChinhKieuSo.class)
    private Double tienTrungBaoLo;
    @JsonSerialize(using = DieuChinhKieuSo.class)
    private Double tienTrungThuong;
    @JsonSerialize(using = DieuChinhKieuSo.class)
    private Double tienTrungDacBiet;


    // ✅ Danh sách tên đài (nếu chỉ muốn tên)
    private List<String> danhSachDai;

    // ✅ Danh sách kết quả chi tiết từng đài
    private List<KetQuaTheoDai> ketQuaTungDai;

    // ✅ MỚI: Danh sách kết quả chi tiết từng lần trúng (gộp toàn bộ các đài/giải)
    private List<KetQuaTrungDto> ketQuaChiTiet;

    @Getter
    @Setter
    public static class KetQuaTheoDai {
        private String tenDai;
        private String mien;
        private boolean trung;
        @JsonSerialize(using = DieuChinhKieuSo.class)
        private double tienTrung;
        private String soTrung;
        private List<String> giaiTrung;
        private String lyDo;
        private int soLanTrung;

    }

}
