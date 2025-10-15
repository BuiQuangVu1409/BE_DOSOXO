package com.example.doxoso.model;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTongTienHH {


        private Long playerId;
        private String playerName;
        private LocalDate ngay;

        // ✅ Hệ số hoa hồng đã chuẩn hoá về tỉ lệ 0.xx (69% -> 0.69)
        private BigDecimal heSoHoaHong;
//        private String heSoHoaHong%; // ví dụ "69%" hoặc "69.5%"

        // Tổng tiền đánh đã nhân hoa hồng (TỔNG * hệ số)
        private BigDecimal tongDaNhanHoaHong;

        // Tổng hoa hồng theo miền
        private BigDecimal hoaHongMB;  // MIEN BAC * hệ số
        private BigDecimal hoaHongMT;  // MIEN TRUNG * hệ số
        private BigDecimal hoaHongMN;  // MIEN NAM * hệ số

}
