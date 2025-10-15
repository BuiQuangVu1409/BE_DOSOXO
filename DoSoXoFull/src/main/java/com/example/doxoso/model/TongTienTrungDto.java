package com.example.doxoso.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TongTienTrungDto {

        private Long playerId;
        private String ngay;              // yyyy-MM-dd (string cho gọn JSON)
        private BigDecimal tongToanBo;    // tổng tất cả miền
        private List<MienDto> cacMien;

        @Data
        public static class MienDto {
            private String mien;              // MB | MT | MN
            private BigDecimal tongTienMien;  // sum tất cả đài trong miền
            private List<DaiDto> cacDai;
        }

        @Data
        public static class DaiDto {
            private String tenDai;
            private BigDecimal tongTienDai;   // sum tất cả cách đánh của đài
            private List<CachDanhDto> cacCachDanh;
        }

        @Data
        public static class CachDanhDto {
            private String cachDanh;          // LỚN, NHỎ, XUYÊN, 2 CHÂN, 3 CHÂN, ĐẦU, ĐUÔI, ĐẦU ĐUÔI...
            private BigDecimal tienTrung;     // số tiền trúng của riêng cách đánh này
        }
    }


