package com.example.doxoso.controller;

import com.example.doxoso.model.KetQuaNguoiChoi;
import com.example.doxoso.service.KetQuaNguoiChoiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/api/ketqua")
public class KetQuaNguoiChoiController {

    private final KetQuaNguoiChoiService ketQuaNguoiChoiService;

    public KetQuaNguoiChoiController(KetQuaNguoiChoiService ketQuaNguoiChoiService) {
        this.ketQuaNguoiChoiService = ketQuaNguoiChoiService;
    }

    // Lấy theo playerId (path clear)
    // GET /api/ketqua/by-player-id/2
    @GetMapping("/by-player-id/{playerId}")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerId(@PathVariable Long playerId) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerId(playerId));
    }

    // Lấy theo playerName (dùng query param để tránh lỗi encode ký tự có dấu)
    // GET /api/ketqua/by-player-name?name=Đại%20Tâm
    @GetMapping("/by-player-name")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerName(@RequestParam("name") String playerName) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerName(playerName));
    }

    // Lấy theo ngày chơi
    // GET /api/ketqua/by-date/2025-08-23
    @GetMapping("/by-date/{ngayChoi}")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByNgayChoi(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayChoi) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByNgayChoi(ngayChoi));
    }

    // Lấy theo playerId + ngày (dùng query param cho rõ)
    // GET /api/ketqua/by-player-id-and-date?playerId=2&date=2025-06-25
    @GetMapping("/by-player-id-and-date")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerIdAndNgay(
            @RequestParam Long playerId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerIdAndNgay(playerId, ngay));
    }

    // Lấy theo playerName + ngày
    // GET /api/ketqua/by-player-name-and-date?name=Đại%20Tâm&date=2025-08-23
    @GetMapping("/by-player-name-and-date")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByPlayerNameAndNgay(
            @RequestParam("name") String playerName,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayChoi) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getByPlayerNameAndNgay(playerName, ngayChoi));
    }

    // Lấy theo khoảng ngày
    // GET /api/ketqua/by-range?from=2025-08-01&to=2025-08-15
    @GetMapping("/by-range")
    public ResponseEntity<List<KetQuaNguoiChoi>> getByRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ketQuaNguoiChoiService.getKetQuaTrongKhoang(startDate, endDate));
    }
}
