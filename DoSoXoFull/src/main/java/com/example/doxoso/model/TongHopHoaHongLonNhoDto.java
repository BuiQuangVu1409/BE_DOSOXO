
package com.example.doxoso.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TongHopHoaHongLonNhoDto {

    private Long playerId;
    private String playerName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate ngay;

    // (A) Tổng đã *nhân hoa hồng* (lấy từ TongTienHHService)
    private BigDecimal tongDaNhanHoaHong;
    private BigDecimal tongDaNhanHoaHongMB;
    private BigDecimal tongDaNhanHoaHongMT;
    private BigDecimal tongDaNhanHoaHongMN;

    // (B) Tiền LỚN/NHỎ cộng thêm theo miền
    private BigDecimal tienLonNhoMB;
    private BigDecimal tienLonNhoMT;
    private BigDecimal tienLonNhoMN;
    private BigDecimal tongLonNho;

    // (C) Tổng cộng = (A) + (B)
    private BigDecimal tongCongMB;
    private BigDecimal tongCongMT;
    private BigDecimal tongCongMN;
    private BigDecimal tongCong;
}
