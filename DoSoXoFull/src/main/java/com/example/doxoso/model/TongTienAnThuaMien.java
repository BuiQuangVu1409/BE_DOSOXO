// com.example.doxoso.model.TongTienAnThuaMien.java
package com.example.doxoso.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TongTienAnThuaMien {
    private String mien;             // "MIỀN BẮC" | "MIỀN TRUNG" | "MIỀN NAM"
    private BigDecimal tongTrung;    // tổng tiền trúng của miền
    private BigDecimal tongHH;       // tổng hoa hồng của miền
    private BigDecimal lonNho;       // tổng lớn/nhỏ của miền
    private BigDecimal tongAnThua;   // tongTrung - (tongHH + lonNho)
}
