package com.example.doxoso.controller;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.PlayerDoKetQuaDto;
import com.example.doxoso.service.BetService;
import com.example.doxoso.service.KetQuaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173","http://localhost:3000","http://localhost:3001"})
@RestController
@RequestMapping(
        path = {"/api/bets", "/api/songuoichoi"},
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class BetController {

    private final BetService betService;
    private final KetQuaService ketQuaService;

    public BetController(BetService betService, KetQuaService ketQuaService) {
        this.betService = betService;
        this.ketQuaService = ketQuaService;
    }

    // ===== READ =====

    @GetMapping("/{id}")
    public ResponseEntity<Bet> getById(@PathVariable Long id) {
        return ResponseEntity.ok(betService.getById(id));
    }

    /**
     * Tìm kiếm linh hoạt:
     * /api/bets?playerId=&from=&to=&mien=&dai=
     */
    @GetMapping
    public ResponseEntity<List<Bet>> search(
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String mien,
            @RequestParam(required = false) String dai
    ) {
        return ResponseEntity.ok(betService.search(playerId, from, to, mien, dai));
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Bet>> getByPlayer(@PathVariable Long playerId) {
        return ResponseEntity.ok(betService.getByPlayerId(playerId));
    }

    // ===== CREATE / UPDATE / DELETE =====

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Bet> create(@RequestBody @Valid Bet req) {
        return ResponseEntity.ok(betService.create(req));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Bet> replace(@PathVariable Long id, @RequestBody @Valid Bet req) {
        return ResponseEntity.ok(betService.replace(id, req));
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Bet> patch(@PathVariable Long id, @RequestBody Bet req) {
        return ResponseEntity.ok(betService.patch(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        betService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{betId}/player/{playerId}")
    public ResponseEntity<Bet> changePlayer(@PathVariable Long betId, @PathVariable Long playerId) {
        return ResponseEntity.ok(betService.changePlayer(betId, playerId));
    }

    // ===== PATCH tiện dụng =====

    @PatchMapping("/{id}/sotien")
    public ResponseEntity<Bet> updateSoTien(@PathVariable Long id, @RequestParam String soTien) {
        return ResponseEntity.ok(betService.updateSoTien(id, soTien));
    }

    @PatchMapping("/{id}/sodanh")
    public ResponseEntity<Bet> updateSoDanh(@PathVariable Long id, @RequestParam String soDanh) {
        return ResponseEntity.ok(betService.updateSoDanh(id, soDanh));
    }

    @PatchMapping("/{id}/cachdanh")
    public ResponseEntity<Bet> updateCachDanh(@PathVariable Long id, @RequestParam String cachDanh) {
        return ResponseEntity.ok(betService.updateCachDanh(id, cachDanh));
    }

    // PATCH /api/bets/{id}/mien?code=MT
    @PatchMapping("/{id}/mien")
    public ResponseEntity<Bet> updateMien(@PathVariable Long id, @RequestParam String code) {
        String mien = switch (code.trim().toUpperCase()) {
            case "MB", "BAC", "MIEN_BAC"     -> "MIỀN BẮC";
            case "MT", "TRUNG", "MIEN_TRUNG" -> "MIỀN TRUNG";
            case "MN", "NAM", "MIEN_NAM"     -> "MIỀN NAM";
            default -> throw new IllegalArgumentException("Mã miền không hợp lệ: " + code);
        };
        return ResponseEntity.ok(betService.updateMien(id, mien));
    }

    @PatchMapping("/{id}/dai")
    public ResponseEntity<Bet> updateDai(@PathVariable Long id, @RequestParam String dai) {
        return ResponseEntity.ok(betService.updateDai(id, dai));
    }

    @PatchMapping("/{id}/ngay")
    public ResponseEntity<Bet> updateNgay(@PathVariable Long id,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return ResponseEntity.ok(betService.updateNgay(id, ngay));
    }

    @PatchMapping("/{id}/player/{playerId}")
    public ResponseEntity<Bet> changePlayerPatch(@PathVariable Long id, @PathVariable Long playerId) {
        return ResponseEntity.ok(betService.changePlayer(id, playerId));
    }

    // ===== DÒ KẾT QUẢ THEO PLAYER =====

    @GetMapping("/player/{playerId}/ket-qua")
    public ResponseEntity<PlayerDoKetQuaDto> ketQuaByPlayer(@PathVariable Long playerId) {
        List<Bet> list = betService.getByPlayerId(playerId);
        if (list.isEmpty()) {
            // tuỳ ý: trả 404/200
            return ResponseEntity.ok(new PlayerDoKetQuaDto()); // hoặc throw not found
        }
        var player = list.get(0).getPlayer();
        List<DoiChieuKetQuaDto> ketQua = ketQuaService.doKetQua(list);

        PlayerDoKetQuaDto dto = new PlayerDoKetQuaDto();
        dto.setPlayerId(player.getId());
        dto.setName(player.getName());
        dto.setHoaHong(player.getHoaHong());
        dto.setHeSoCachDanh(player.getHeSoCachDanh());
        dto.setKetQua(ketQua);
        return ResponseEntity.ok(dto);
    }

    // ===== Exception mapping gọn =====
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý yêu cầu.";
        if (msg.toLowerCase().contains("không tìm thấy")) {
            return ResponseEntity.status(404).body(msg);
        }
        return ResponseEntity.badRequest().body(msg);
    }
}
