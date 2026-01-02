package com.example.doxoso.model;  // hoặc dto nếu bạn tách dto riêng

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BangKetQuaDaiDto {
    private String mien;
    private String tenDai;
    private LocalDate ngay;
    private String thu;
    private Map<String, List<String>> bangKetQua;
}
