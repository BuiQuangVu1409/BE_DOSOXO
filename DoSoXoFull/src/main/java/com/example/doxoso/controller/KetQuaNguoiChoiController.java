package com.example.doxoso.controller;

import com.example.doxoso.model.KetQuaNguoiChoi;
import com.example.doxoso.service.KetQuaNguoiChoiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/api/ketqua")
@RequiredArgsConstructor
public class KetQuaNguoiChoiController {

    private final KetQuaNguoiChoiService ketQuaNguoiChoiService;

    // ================== 1. Theo playerId ==================
    // GET /api/ketqua/by-player-id/2
    @GetMapping("/by-player-id/{playerId}")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerId(@PathVariable Long playerId) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerId(playerId));
    }

    // ================== 2. Theo playerName ==================
    // GET /api/ketqua/by-player-name?name=LÍP
    @GetMapping("/by-player-name")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerName(
            @RequestParam("name") String playerName
    ) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerName(playerName));
    }

    // ================== 3. Theo ngày chơi ==================
    // GET /api/ketqua/by-date/2025-09-15
    @GetMapping("/by-date/{ngayChoi}")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByNgayChoi(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayChoi
    ) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByNgayChoi(ngayChoi));
    }

    // ================== 4. Theo playerId + ngày ==================
    // GET /api/ketqua/by-player-id-and-date?playerId=2&ngay=2025-09-15
    @GetMapping("/by-player-id-and-date")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerIdAndNgay(
            @RequestParam("playerId") Long playerId,
            @RequestParam("ngay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerIdAndNgay(playerId, ngay));
    }

    // ================== 5. Theo playerName + ngày ==================
    // GET /api/ketqua/by-player-name-and-date?name=LÍP&ngay=2025-09-15
    @GetMapping("/by-player-name-and-date")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerNameAndNgay(
            @RequestParam("name") String playerName,
            @RequestParam("ngay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayChoi
    ) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerNameAndNgay(playerName, ngayChoi));
    }

    // ================== 6. Theo khoảng ngày ==================
    // GET /api/ketqua/by-range?from=2025-09-01&to=2025-09-15
    // hoặc GET /api/ketqua/by-range?from=2025-09-15  (to=null -> =from)
    @GetMapping("/by-range")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (endDate == null) {
            endDate = startDate;
        }
        return ResponseEntity.ok(ketQuaNguoiChoiService.getKetQuaTrongKhoang(startDate, endDate));
    }
}
