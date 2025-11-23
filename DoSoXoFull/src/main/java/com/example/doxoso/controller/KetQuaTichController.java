package com.example.doxoso.controller;

import com.example.doxoso.model.KetQuaTich;
import com.example.doxoso.repository.BetRepository;
import com.example.doxoso.repository.KetQuaTichRepository;
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
    private final KetQuaTichRepository ketQuaTichRepo;

    /**
     * API chính: lấy KQT đã lưu hoặc auto chạy.
     *
     * auto=true   → nếu chưa có snapshot thì tự run.
     * refresh=true → luôn luôn tính lại + lưu.
     */
    @GetMapping("/{playerId}/{ngay}")
    public ResponseEntity<?> getOrRun(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(defaultValue = "false") boolean auto,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        try {
            // 1️⃣ Nếu refresh → bỏ qua snapshot cũ → tính lại luôn
            if (refresh) {
                List<KetQuaTich> rows = service.runAndSaveForPlayer(playerId, null, ngay);
                return ResponseEntity.ok(rows);
            }

            // 2️⃣ Lấy snapshot nếu có
            List<KetQuaTich> snapshot = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);

            // 3️⃣ Nếu chưa có & auto=true → tự chạy & trả về
            if ((snapshot == null || snapshot.isEmpty()) && auto) {
                snapshot = service.runAndSaveForPlayer(playerId, null, ngay);
            }

            return ResponseEntity.ok(snapshot == null ? List.of() : snapshot);

        } catch (Exception e) {
            return error(e);
        }
    }

    /** API refresh dạng GET */
    @GetMapping("/{playerId}/{ngay}/refresh")
    public ResponseEntity<?> hardRefresh(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        try {
            return ResponseEntity.ok(service.runAndSaveForPlayer(playerId, null, ngay));
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Kiểm tra snapshot có tồn tại chưa */
    @GetMapping("/exists/{playerId}/{ngay}")
    public ResponseEntity<?> exists(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        try {
            List<KetQuaTich> list = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
            Map<String, Object> body = new LinkedHashMap<>();
            int count = list == null ? 0 : list.size();

            body.put("exists", count > 0);
            body.put("count", count);

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            return error(e);
        }
    }

    /** Chạy & lưu KQT 1 player */
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

    /** Chạy & lưu ALL player trong ngày */
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

    // -------- ERROR --------
    private ResponseEntity<Map<String, Object>> error(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getClass().getSimpleName());
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
