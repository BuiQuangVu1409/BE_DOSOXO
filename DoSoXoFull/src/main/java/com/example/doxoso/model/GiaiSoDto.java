package com.example.doxoso.model;


public class GiaiSoDto {
    private String giai;
    private String soTrung;



    public GiaiSoDto(String giai, String soTrung) {
        this.giai = giai;
        this.soTrung = soTrung;
    }

    // Getters và Setters
    public String getGiai() {
        return giai;
    }

    public void setGiai(String giai) {
        this.giai = giai;
    }

    public String getSoTrung() {
        return soTrung;
    }

    public void setSoTrung(String soTrung) {
        this.soTrung = soTrung;
    }
}

