package com.example.doxoso.controller;

import com.example.doxoso.model.PlayerTongTienDanhTheoMienDto;
import com.example.doxoso.service.TongTienDanhTheoMienService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:5173"
})
@RestController
@RequestMapping("/tong-tien")
@RequiredArgsConstructor
public class TongTienDanhTheoMienController {

    private final TongTienDanhTheoMienService tongTienService;

    /**
     * ✅ Tổng tiền đánh theo miền cho 1 player (toàn bộ lịch sử)
     * GET /tong-tien/player/{playerId}
     */
    @GetMapping("/player/{playerId}")
    public PlayerTongTienDanhTheoMienDto getTongTienTheoMien(@PathVariable Long playerId) {
        return tongTienService.tinhTongTheoMien(playerId);
    }

    /**
     * ✅ Tổng tiền đánh theo miền cho TẤT CẢ player
     * GET /tong-tien/players
     */
    @GetMapping("/players")
    public List<PlayerTongTienDanhTheoMienDto> getTongTienTatCaPlayer() {
        return tongTienService.tinhTatCaPlayer();
    }

    /**
     * ✅ Tổng tiền theo *khoảng ngày* cho 1 player
     * (đã loại LỚN/NHỎ/LỚN-NHỎ)
     *
     * Ví dụ:
     *  GET /tong-tien/player/2/ngay?from=2025-09-01&to=2025-09-30
     *  GET /tong-tien/player/2/ngay?from=2025-09-15          (to = from)
     */
    @GetMapping("/player/{playerId}/ngay")
    public List<PlayerTongTienDanhTheoMienDto> getTongTheoMienTheoKhoangNgay(
            @PathVariable Long playerId,
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (to == null) {
            to = from;  // 🔸 GIỮ NGUYÊN LOGIC: nếu không truyền "to" thì hiểu là 1 ngày
        }
        return tongTienService.tinhTongTheoMienTheoNgay(playerId, from, to);
    }

    /**
     * 🆕 NEW: Tổng tiền theo *một ngày duy nhất* cho 1 player
     *
     * Thuận tiện cho FE: chỉ cần truyền ngày trên path, không cần from/to.
     *
     * Ví dụ:
     *  GET /tong-tien/player/2/ngay/2025-09-15
     */
    @GetMapping("/player/{playerId}/ngay/{ngay}")
    public List<PlayerTongTienDanhTheoMienDto> getTongTheoMienMotNgay(
            @PathVariable Long playerId,
            @PathVariable("ngay")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        // Gọi lại service hiện có, from = to = ngay
        return tongTienService.tinhTongTheoMienTheoNgay(playerId, ngay, ngay);
    }
}
