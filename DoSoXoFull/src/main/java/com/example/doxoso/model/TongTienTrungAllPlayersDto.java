
package com.example.doxoso.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TongTienTrungAllPlayersDto {
    private String ngay;                 // yyyy-MM-dd
    private BigDecimal grandTotal;       // tổng của tất cả players
    private List<PlayerBlock> players;   // mỗi phần tử = 1 player

    @Data
    public static class PlayerBlock {
        private Long playerId;
        private String playerName;       // nếu bạn có tên trong DB thì set; không có thì để null
        private BigDecimal tongToanBo;   // tổng của player này
        private List<TongTienTrungDto.MienDto> cacMien; // TÁI DÙNG DTO cũ (miền→đài→cách đánh)
    }
}
