package com.example.doxoso.controller;

import com.example.doxoso.model.PlayerTongTienHH;
import com.example.doxoso.service.TongTienHHService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/hoa-hong")
@RequiredArgsConstructor
public class TongTienHHController {



        private final TongTienHHService hoaHongService;

        @GetMapping("/player/{playerId}")
        public PlayerTongTienHH tinhHoaHongPlayer(@PathVariable Long playerId) {
            return hoaHongService.tinhHoaHongTheoMien(playerId);
        }
        // 👉 Tính hoa hồng cho TẤT CẢ player
        @GetMapping("/players")
        public List<PlayerTongTienHH> tinhHoaHongTatCa() {
            return hoaHongService.tinhHoaHongTatCaPlayer();
        }
        // 👉 Hoa hồng theo *ngày*
        @GetMapping("/player/{playerId}/ngay")
        public List<PlayerTongTienHH> hoaHongTheoNgay(
                @PathVariable Long playerId,
                @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                @RequestParam(value="to", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            if (to == null) to = from;
            return hoaHongService.tinhHoaHongTheoNgay(playerId, from, to);
        }

}


