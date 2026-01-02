package com.example.doxoso.model;

import com.example.doxoso.model.BangKetQuaDaiDto;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichDoMienDto {
    private String mien;                 // MIỀN BẮC
    private LocalDate ngay;
    private String thu;
    private List<BangKetQuaDaiDto> danhSachDai; // nhiều đài trong MT/MN
}

