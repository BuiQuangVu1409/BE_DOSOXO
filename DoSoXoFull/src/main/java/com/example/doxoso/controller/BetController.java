package com.example.doxoso.controller;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.PlayerDoKetQuaDto;
import com.example.doxoso.service.BetService;
import com.example.doxoso.service.KetQuaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * =========================================
 *  BET CONTROLLER (REST)
 *  - Prefix hỗ trợ song song:
 *      + Mới:  /api/bets
 *      + Cũ:   /api/songuoichoi
 *
 *  ✅ URL nhanh để dùng (có cả ví dụ):
 *
 *  [READ]
 *  - GET /api/bets/{id}
 *      vd: GET http://localhost:8080/api/bets/10
 *
 *  - GET /api/bets?playerId=1&from=2025-01-01&to=2025-01-31&mien=MIEN%20NAM&dai=Khanh
 *      (Tất cả query param đều optional; truyền cái bạn cần)
 *
 *  - GET /api/bets/player/{playerId}
 *      vd: GET http://localhost:8080/api/bets/player/1
 *
 *  [CREATE/UPDATE/DELETE]
 *  - POST /api/bets
 *      Body (JSON) tạo dùng player có sẵn:
 *      {
 *        "mien":"MIỀN NAM","dai":"Hồ Chí Minh","cachDanh":"ĐẦU",
 *        "soDanh":"12","soTien":"100000",
 *        "player":{"id":1}
 *      }
 *      Body (JSON) tạo kèm player mới:
 *      {
 *        "mien":"MIỀN BẮC","dai":"Hà Nội","cachDanh":"ĐUÔI",
 *        "soDanh":"45","soTien":"50000",
 *        "player":{"name":"Nguyễn A","hoaHong":0.05,"heSoCachDanh":1.95}
 *      }
 *
 *  - PUT /api/bets/{id}
 *      (thay thế toàn bộ trường — giống POST nhưng vào bet đã có)
 *
 *  - PATCH /api/bets/{id}
 *      (cập nhật một phần; ví dụ chỉ đổi tiền)
 *      { "soTien":"75000" }
 *
 *  - DELETE /api/bets/{id}
 *
 *  [PATCH RIÊNG LẺ – dễ test bằng URL]
 *  - PATCH /api/bets/{id}/sotien?soTien=85000
 *  - PATCH /api/bets/{id}/sodanh?soDanh=12-34
 *  - PATCH /api/bets/{id}/cachdanh?cachDanh=DAU
 *  - PATCH /api/bets/{id}/mien?mien=MIỀN%20TRUNG
 *  - PATCH /api/bets/{id}/dai?dai=Khánh%20Hòa
 *  - PATCH /api/bets/{id}/ngay?ngay=2025-09-30
 *  - PATCH /api/bets/{id}/player/{playerId}
 *
 *  [DÒ KẾT QUẢ THEO PLAYER]
 *  - GET /api/bets/player/{playerId}/ket-qua
 *
 *  Lưu ý:
 *  - Controller chỉ gọi service; toàn bộ nghiệp vụ/validate ở tầng service.
 *  - Model Bet/Player đã dùng @JsonManagedReference/@JsonBackReference để
 *    tránh vòng lặp khi trả JSON (nếu bạn chọn cách đó).
 * =========================================
 */
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController // đánh dấu lớp là REST Controller (trả JSON)
@RequestMapping({"/api/bets", "/api/songuoichoi"}) // map 2 prefix để tương thích ngược
public class BetController {

    // ====== DI (Dependency Injection) bằng constructor ======
    private final BetService betService;     // service xử lý CRUD Bet
    private final KetQuaService ketQuaService; // service dò kết quả cho danh sách Bet

    // Spring sẽ tự inject 2 bean này (BetService, KetQuaService)
    public BetController(BetService betService, KetQuaService ketQuaService) {
        this.betService = betService;
        this.ketQuaService = ketQuaService;
    }

    // ========= READ =========

    /**
     * Lấy 1 bet theo id
     * GET /api/bets/{id}
     * @param id id bet
     * @return Bet (200) hoặc lỗi 404 khi không tìm thấy (được handle ở @ExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Bet> getById(@PathVariable Long id) {
        return ResponseEntity.ok(betService.getById(id));
    }

    /**
     * Tìm kiếm linh hoạt theo các tiêu chí:
     * - playerId (optional)
     * - from/to (LocalDate, optional)
     * - mien, dai (chuỗi LIKE, optional)
     * GET /api/bets?playerId=&from=&to=&mien=&dai=
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

    /**
     * Lấy toàn bộ bet theo playerId
     * GET /api/bets/player/{playerId}
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Bet>> getByPlayer(@PathVariable Long playerId) {
        return ResponseEntity.ok(betService.getByPlayerId(playerId));
    }

    // ========= CREATE / UPDATE / DELETE =========

    /**
     * Tạo bet mới (support 2 cách gán player: id có sẵn hoặc tạo player mới từ body)
     * POST /api/bets
     * Body: JSON của Bet (xem block URL ở đầu file)
     */
    @PostMapping
    public ResponseEntity<Bet> create(@RequestBody Bet req) {
        return ResponseEntity.ok(betService.create(req));
    }

    /**
     * Thay thế toàn bộ trường của bet (PUT semantics)
     * PUT /api/bets/{id}
     * Body: JSON đầy đủ các trường (như POST)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Bet> replace(@PathVariable Long id, @RequestBody Bet req) {
        return ResponseEntity.ok(betService.replace(id, req));
    }

    /**
     * Cập nhật một phần (PATCH semantics)
     * PATCH /api/bets/{id}
     * Body: chỉ các field muốn đổi (vd: { "soTien": "75000" })
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Bet> patch(@PathVariable Long id, @RequestBody Bet req) {
        return ResponseEntity.ok(betService.patch(id, req));
    }

    /**
     * Xóa 1 bet
     * DELETE /api/bets/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        betService.delete(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * Đổi bet sang player khác (PUT)
     * PUT /api/bets/{betId}/player/{playerId}
     */
    @PutMapping("/{betId}/player/{playerId}")
    public ResponseEntity<Bet> changePlayer(@PathVariable Long betId, @PathVariable Long playerId) {
        return ResponseEntity.ok(betService.changePlayer(betId, playerId));
    }

    // ========= PATCH RIÊNG LẺ (dễ thao tác nhanh bằng URL) =========

    /** PATCH /api/bets/{id}/sotien?soTien=85000 */
    @PatchMapping("/{id}/sotien")
    public ResponseEntity<Bet> updateSoTien(@PathVariable Long id, @RequestParam String soTien) {
        return ResponseEntity.ok(betService.updateSoTien(id, soTien));
    }

    /** PATCH /api/bets/{id}/sodanh?soDanh=12-34 */
    @PatchMapping("/{id}/sodanh")
    public ResponseEntity<Bet> updateSoDanh(@PathVariable Long id, @RequestParam String soDanh) {
        return ResponseEntity.ok(betService.updateSoDanh(id, soDanh));
    }

    /** PATCH /api/bets/{id}/cachdanh?cachDanh=DAU */
    @PatchMapping("/{id}/cachdanh")
    public ResponseEntity<Bet> updateCachDanh(@PathVariable Long id, @RequestParam String cachDanh) {
        return ResponseEntity.ok(betService.updateCachDanh(id, cachDanh));
    }

    // PATCH /api/bets/{id}/mien?code=MT
    @PatchMapping("/{id}/mien")
    public ResponseEntity<Bet> updateMien(@PathVariable Long id,
                                          @RequestParam String code) {
        String mien = switch (code.trim().toUpperCase()) {
            case "MB", "BAC", "MIEN_BAC"     -> "MIỀN BẮC";
            case "MT", "TRUNG", "MIEN_TRUNG" -> "MIỀN TRUNG";
            case "MN", "NAM", "MIEN_NAM"     -> "MIỀN NAM";
            default -> throw new IllegalArgumentException("Mã miền không hợp lệ: " + code);
        };
        return ResponseEntity.ok(betService.updateMien(id, mien));
    }


    /** PATCH /api/bets/{id}/dai?dai=Khánh%20Hòa */
    @PatchMapping("/{id}/dai")
    public ResponseEntity<Bet> updateDai(@PathVariable Long id, @RequestParam String dai) {
        return ResponseEntity.ok(betService.updateDai(id, dai));
    }

    /** PATCH /api/bets/{id}/ngay?ngay=2025-09-30 */
    @PatchMapping("/{id}/ngay")
    public ResponseEntity<Bet> updateNgay(@PathVariable Long id,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return ResponseEntity.ok(betService.updateNgay(id, ngay));
    }

    /** PATCH /api/bets/{id}/player/{playerId} */
    @PatchMapping("/{id}/player/{playerId}")
    public ResponseEntity<Bet> changePlayerPatch(@PathVariable Long id, @PathVariable Long playerId) {
        return ResponseEntity.ok(betService.changePlayer(id, playerId));
    }

    // ========= DÒ KẾT QUẢ THEO PLAYER =========

    /**
     * Dò kết quả cho toàn bộ bet của 1 player và trả về DTO tổng hợp
     * GET /api/bets/player/{playerId}/ket-qua
     */
    @GetMapping("/player/{playerId}/ket-qua")
    public ResponseEntity<PlayerDoKetQuaDto> ketQuaByPlayer(@PathVariable Long playerId) {
        List<Bet> list = betService.getByPlayerId(playerId);         // lấy danh sách bet theo player
        var player = list.get(0).getPlayer();
        List<DoiChieuKetQuaDto> ketQua = ketQuaService.doKetQua(list);// dò kết quả từng bet

        // Gói về DTO cho gọn (tránh vòng tham chiếu khi trả nguyên entity Player)
        PlayerDoKetQuaDto dto = new PlayerDoKetQuaDto();
        dto.setPlayerId(player.getId());
        dto.setName(player.getName());
        dto.setHoaHong(player.getHoaHong());
        dto.setHeSoCachDanh(player.getHeSoCachDanh());
        dto.setKetQua(ketQua);
        return ResponseEntity.ok(dto);
    }

    // ========= Bắt lỗi RuntimeException và map thành HTTP code hợp lý =========
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý yêu cầu.";
        // Nếu message có cụm “không tìm thấy” → 404; ngược lại trả 400 Bad Request
        if (msg.toLowerCase().contains("không tìm thấy")) {
            return ResponseEntity.status(404).body(msg);
        }
        return ResponseEntity.badRequest().body(msg);
    }
}
