package com.example.doxoso.controller;

import com.example.doxoso.model.KetQuaTich;
import com.example.doxoso.repository.BetRepository;
import com.example.doxoso.service.KetQuaTichService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(
        origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        allowedHeaders = "*"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/ket-qua-tich")
public class KetQuaTichController {

    private final KetQuaTichService service;
    private final BetRepository betRepository;

    /**
     * Lấy snapshot đã lưu (3 dòng/miền) cho 1 player trong 1 ngày.
     * Query params:
     *  - auto=true   : nếu chưa có thì tự run & save rồi trả về
     *  - refresh=true: luôn run & save lại (bỏ qua snapshot cũ)
     * Luôn trả về 200 + JSON array (kể cả rỗng) để FE dễ xử lý.
     */
    @GetMapping(value = "/{playerId}/{ngay}", produces = "application/json")
    public ResponseEntity<?> getOrRun(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(name = "auto", required = false, defaultValue = "false") boolean autoCreate,
            @RequestParam(name = "refresh", required = false, defaultValue = "false") boolean refresh
    ) {
        try {
            if (refresh) {
                List<KetQuaTich> fresh = service.runAndSaveForPlayer(playerId, null, ngay);
                return ResponseEntity.ok(fresh);
            }
            List<KetQuaTich> snapshot = service.findByPlayerAndNgay(playerId, ngay);
            if ((snapshot == null || snapshot.isEmpty()) && autoCreate) {
                snapshot = service.runAndSaveForPlayer(playerId, null, ngay);
            }
            return ResponseEntity.ok(snapshot == null ? List.of() : snapshot);
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Alias refresh dạng GET cho dễ gọi từ trình duyệt */
    @GetMapping(value = "/{playerId}/{ngay}/refresh", produces = "application/json")
    public ResponseEntity<?> refreshOne(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        try {
            return ResponseEntity.ok(service.runAndSaveForPlayer(playerId, null, ngay));
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Kiểm tra tồn tại snapshot (trả về {exists, count}) */
    @GetMapping(value = "/exists/{playerId}/{ngay}", produces = "application/json")
    public ResponseEntity<?> exists(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        try {
            List<KetQuaTich> snapshot = service.findByPlayerAndNgay(playerId, ngay);
            Map<String, Object> body = new LinkedHashMap<>();
            int count = snapshot == null ? 0 : snapshot.size();
            body.put("exists", count > 0);
            body.put("count", count);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Chạy & lưu 3 miền cho 1 player trong 1 ngày. */
    @PostMapping("/run-save/{playerId}/{ngay}")
    public ResponseEntity<?> runSaveOne(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) String playerName
    ) {
        try {
            return ResponseEntity.ok(service.runAndSaveForPlayer(playerId, playerName, ngay));
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Chạy & lưu đồng loạt cho tất cả player có cược trong ngày. */
    @PostMapping("/run-save-all/{ngay}")
    public ResponseEntity<?> runSaveAll(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        try {
            var ids = betRepository.findDistinctPlayerIdsByNgay(ngay);
            Map<Long, List<KetQuaTich>> out = new LinkedHashMap<>();
            for (Long pid : ids) {
                out.put(pid, service.runAndSaveForPlayer(pid, null, ngay));
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return error(e);
        }
    }

    // -------- helpers --------
    private ResponseEntity<Map<String, Object>> error(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getClass().getSimpleName());
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
