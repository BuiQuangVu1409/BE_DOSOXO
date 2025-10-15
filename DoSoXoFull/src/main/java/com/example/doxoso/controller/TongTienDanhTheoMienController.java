package com.example.doxoso.controller;
import com.example.doxoso.model.PlayerTongTienDanhTheoMienDto;
import com.example.doxoso.service.TongTienDanhTheoMienService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/tong-tien")
@RequiredArgsConstructor
public class TongTienDanhTheoMienController {


        private final TongTienDanhTheoMienService tongTienService;

        // 1 player → 1 DTO
        @GetMapping("/player/{playerId}")
        public PlayerTongTienDanhTheoMienDto getTongTienTheoMien(@PathVariable Long playerId) {
            return tongTienService.tinhTongTheoMien(playerId);
        }

        // tất cả player → List<DTO>
        @GetMapping("/players")
        public List<PlayerTongTienDanhTheoMienDto> getTongTienTatCaPlayer() {
            return tongTienService.tinhTatCaPlayer();
        }

    // 👉 Tổng tiền theo *ngày* (đã loại LỚN/NHỎ/LỚN-NHỎ)
    @GetMapping("/player/{playerId}/ngay")
    public List<PlayerTongTienDanhTheoMienDto> tongTheoNgay(
            @PathVariable Long playerId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value="to", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to == null) to = from;
        return tongTienService.tinhTongTheoMienTheoNgay(playerId, from, to);
    }

}



