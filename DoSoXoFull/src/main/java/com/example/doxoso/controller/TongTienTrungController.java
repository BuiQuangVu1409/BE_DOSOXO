package com.example.doxoso.controller;

import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.TongTienTrungAllPlayersDto;
import com.example.doxoso.model.TongTienTrungDto;
import com.example.doxoso.service.TongTienTrungService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/tonghop/tientrung")

public class TongTienTrungController {
        private final TongTienTrungService tongTienTrungService;

        // Trường hợp bạn POST list kết quả vừa dò
        @PostMapping("/from-dtos/{playerId}/{ngay}")
        public TongTienTrungDto tongHopFromDtos(
                @PathVariable Long playerId,
                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                @RequestBody List<DoiChieuKetQuaDto> ketQua) {
            return tongTienTrungService.tongHopTuKetQuaDtos(playerId, ngay, ketQua);
        }

        //    http://localhost:8080/tonghop/tientrung/1/2025-06-25

        // Trường hợp đọc thẳng từ DB (đã lưu KetQuaNguoiChoi)
        @GetMapping("/{playerId}/{ngay}")
        public TongTienTrungDto tongHopFromDb(
                @PathVariable Long playerId,
                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
            return tongTienTrungService.tongHopTuDb(playerId, ngay);
        }


        @GetMapping("/tong_tien_trung_tat_ca_id/{ngay}")
        public TongTienTrungDto tongHopAllFromDb(
                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
            return tongTienTrungService.tongHopTatCaPlayerTuDb(ngay);
        }

//        @GetMapping("/all/{ngay}")
//        public TongTienTrungAllPlayersDto tongHopGroupByPlayer(
//                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
//            return tongTienTrungService.tongHopTheoTungPlayer(ngay);
//        }

        @GetMapping("/allplayer/{ngay}")
        public TongTienTrungAllPlayersDto tongHopGroupByPlayerTheoMien(
                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                @RequestParam(name = "mien", required = false) String mien // ví dụ: "MN" hoặc "MB,MT"
        )
        {
            return tongTienTrungService.tongHopTheoTungPlayerTheoMien(ngay, mien);
        }

}


