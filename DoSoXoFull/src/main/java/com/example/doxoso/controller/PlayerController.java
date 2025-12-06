package com.example.doxoso.controller;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.Player;
import com.example.doxoso.model.PlayerDoKetQuaDto;
import com.example.doxoso.service.BetService;
import com.example.doxoso.service.KetQuaService;
import com.example.doxoso.service.KiemTraKetQuaService;
import com.example.doxoso.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/api/player")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BetService betService;

    @Autowired
    private KiemTraKetQuaService kiemTraKetQuaService;

    @Autowired
    private KetQuaService ketQuaService;

    // ===================== SEARCH BY NAME =====================
    // GET /api/player/search?keyword=lip
    @GetMapping("/search")
    public ResponseEntity<List<PlayerSimpleDto>> searchPlayers(@RequestParam String keyword) {
        List<Player> list = playerService.searchPlayersByName(keyword);
        List<PlayerSimpleDto> dto = list.stream()
                .map(p -> new PlayerSimpleDto(p.getId(), p.getName()))
                .toList();
        return ResponseEntity.ok(dto);
    }

    // DTO gọn cho FE (id + name)
    public record PlayerSimpleDto(Long id, String name) {}

    // ===================== CREATE (Client cấp ID) =====================
    @PostMapping
    public ResponseEntity<?> addPlayer(@RequestBody Player player) {
        // 1) BẮT BUỘC có ID khi tạo mới
        if (player.getId() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Thiếu ID",
                            "suggestion", "Thêm mới bắt buộc truyền 'id' duy nhất trong body"
                    )
            );
        }

        // 2) Chuẩn hoá/validate dữ liệu nhập
        String name = player.getName() != null ? player.getName().trim() : null;
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Thiếu tên",
                            "suggestion", "Truyền 'name' không rỗng khi tạo Player"
                    )
            );
        }
        player.setName(name);

        // Nếu entity có @Version thì KHÔNG nhận version từ client khi tạo mới
        // player.setVersion(null);

        try {
            Player saved = playerService.createPlayer(player); // service dùng persist để INSERT với ID client cấp

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location).body(saved);

        } catch (IllegalArgumentException ex) {
            // Ví dụ ID đã tồn tại (service đã kiểm tra existsById)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Xung đột dữ liệu", "message", ex.getMessage())
            );
        } catch (DataIntegrityViolationException ex) {
            // Trùng unique key khác (phone, name...) nếu DB có ràng buộc
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Xung đột dữ liệu", "message", ex.getMostSpecificCause().getMessage())
            );
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Hiếm gặp khi create nếu đã cấu hình đúng; vẫn bắt để an toàn
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Xung đột phiên bản", "message", ex.getMessage())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Internal Server Error", "message", ex.getMessage())
            );
        }
    }

    // ===================== READ ALL =====================
    @GetMapping({"", "/"})
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    // ===================== READ by ID (kèm dò kết quả) =====================
    @GetMapping({"/{playerId}", "/{playerId}/"})
    public ResponseEntity<?> getByPlayerId(@PathVariable Long playerId) {
        List<Bet> list = betService.getSoNguoiChoiByPlayerId(playerId);
        if (list == null || list.isEmpty()) {
            // Giữ nguyên hành vi cũ: 404 + text
            return ResponseEntity.status(404).body("Không tìm thấy dữ liệu cho playerId = " + playerId);
        }

        var player = list.get(0).getPlayer();
        List<DoiChieuKetQuaDto> ketQua = ketQuaService.doKetQua(list);

        PlayerDoKetQuaDto response = new PlayerDoKetQuaDto();
        response.setPlayerId(player.getId());
        response.setName(player.getName());
        response.setHoaHong(player.getHoaHong());
        response.setHeSoCachDanh(player.getHeSoCachDanh());
        response.setKetQua(ketQua);

        return ResponseEntity.ok(response);
    }

    // ===================== REPLACE (PUT) =====================
    @PutMapping("/{id}")
    public ResponseEntity<?> replacePlayer(@PathVariable Long id, @RequestBody Player player) {
        Player updated = playerService.replacePlayer(id, player);
        return ResponseEntity.ok(updated);
    }

    // ===================== DELETE =====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== PATCH: hoa hồng =====================
    @PatchMapping("/{id}/hoahong")
    public ResponseEntity<Player> updateHoaHong(@PathVariable Long id, @RequestParam Double hoaHong) {
        return ResponseEntity.ok(playerService.updateHoaHong(id, hoaHong));
    }
    // ví dụ: http://localhost:8080/api/player/10/hoahong?hoaHong=69.5

    // ===================== PATCH: hệ số cách đánh =====================
    @PatchMapping("/{id}/heso")
    public ResponseEntity<Player> updateHeSo(@PathVariable Long id, @RequestParam Double heSo) {
        return ResponseEntity.ok(playerService.updateHeSoCachDanh(id, heSo));
    }
    // ví dụ: http://localhost:8080/api/player/10/heso?heSo=2.5
}
