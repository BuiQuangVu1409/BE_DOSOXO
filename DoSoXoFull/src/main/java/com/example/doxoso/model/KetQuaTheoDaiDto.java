package com.example.doxoso.model;



import java.util.List;

public class KetQuaTheoDaiDto {
    private String mien;
    private String tenDai;
    private List<GiaiSoDto> ketQua;



    public KetQuaTheoDaiDto(String mien, String tenDai, List<? extends Object> danhSachKetQua) {
        this.mien = mien;
        this.tenDai = tenDai;
        this.ketQua = danhSachKetQua.stream()
                .map(obj -> {
                    if (obj instanceof com.example.doxoso.model.KetQuaMienBac kq) {
                        return new GiaiSoDto(kq.getGiai(), kq.getSoTrung());
                    } else if (obj instanceof com.example.doxoso.model.KetQuaMienTrung kq) {
                        return new GiaiSoDto(kq.getGiai(), kq.getSoTrung());
                    } else if (obj instanceof com.example.doxoso.model.KetQuaMienNam kq) {
                        return new GiaiSoDto(kq.getGiai(), kq.getSoTrung());
                    } else {
                        return null;
                    }
                })
                .filter(kq -> kq != null)
                .toList();
    }

    // Getters và Setters
    public String getMien() {
        return mien;
    }

    public void setMien(String mien) {
        this.mien = mien;
    }

    public String getTenDai() {
        return tenDai;
    }

    public void setTenDai(String tenDai) {
        this.tenDai = tenDai;
    }

    public List<GiaiSoDto> getKetQua() {
        return ketQua;
    }

    public void setKetQua(List<GiaiSoDto> ketQua) {
        this.ketQua = ketQua;
    }
}

