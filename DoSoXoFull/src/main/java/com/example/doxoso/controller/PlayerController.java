package com.example.doxoso.controller;


import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.Player;
import com.example.doxoso.model.PlayerDoKetQuaDto;
//import com.example.doxoso.model.SoNguoiChoi;
import com.example.doxoso.service.*;
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
    // Tạo mới Player
    @PostMapping // nếu class @RequestMapping("/api/player") thì giữ nguyên; nếu không, dùng @PostMapping("/api/player")
    public ResponseEntity<?> addPlayer(@RequestBody Player player) {
        // 1) Không cho tạo mới kèm ID (tránh merge)
        if (player.getId() != null) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Không gửi ID khi tạo mới",
                            "suggestion", "Dùng PUT /api/player/{id} để thay thế dữ liệu cũ"
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

        // 3) Ép version = null để đảm bảo persist (đặc biệt khi có @Version)
//        if (player.getVersion() != null) {
//            player.setVersion(null);
//        }

        // (Tuỳ entity, nếu có createdAt/updatedAt từ client gửi nhầm, có thể xoá/để null)
        // player.setCreatedAt(null);
        // player.setUpdatedAt(null);

        try {
            // Khuyến nghị: gọi hàm createPlayer(...) trong service
            // hàm này nên setId(null) lần nữa để chắc chắn persist
            Player saved = playerService.createPlayer(player);

            // 4) Trả 201 + Location header
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location).body(saved);

        } catch (DataIntegrityViolationException ex) {
            // Ví dụ trùng unique key khác (phone, name...) do ràng buộc DB
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Xung đột dữ liệu", "message", ex.getMostSpecificCause().getMessage())
            );
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Trường hợp hiếm nếu vẫn va vào optimistic locking
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Xung đột phiên bản", "message", ex.getMessage())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Internal Server Error", "message", ex.getMessage())
            );
        }
    }

    // Lấy tất cả Player

    @GetMapping({"", "/"})
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    // Lấy theo id / trả về kết quả dò theo playerId
    @GetMapping({"/{playerId}", "/{playerId}/"})
    public ResponseEntity<?> getByPlayerId(@PathVariable Long playerId) {
        List<Bet> list = betService.getSoNguoiChoiByPlayerId(playerId);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(404).body("Không tìm thấy dữ liệu cho playerId = " + playerId);
        }

        var player = list.get(0).getPlayer();

        // ✅ Dùng service trung gian
        List<DoiChieuKetQuaDto> ketQua = ketQuaService.doKetQua(list);

        PlayerDoKetQuaDto response = new PlayerDoKetQuaDto();
        response.setPlayerId(player.getId());
        response.setName(player.getName());
        response.setHoaHong(player.getHoaHong());
        response.setHeSoCachDanh(player.getHeSoCachDanh());
        response.setKetQua(ketQua);

        return ResponseEntity.ok(response);
    }


    @PutMapping("/{id}")
        public ResponseEntity<?> replacePlayer (
                @PathVariable Long id,
                @RequestBody Player player){
            Player updated = playerService.replacePlayer(id, player);
            return ResponseEntity.ok(updated);
        }
        // Xoá Player
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deletePlayer (@PathVariable Long id){
            playerService.deletePlayer(id);
            return ResponseEntity.noContent().build();
        }

        // Cập nhật hoa hồng riêng
        @PatchMapping("/{id}/hoahong")
        public ResponseEntity<Player> updateHoaHong (@PathVariable Long id, @RequestParam Double hoaHong){
            return ResponseEntity.ok(playerService.updateHoaHong(id, hoaHong));
        }
//    http://localhost:8080/api/player/10/hoahong?hoaHong=69.5
        // Cập nhật hệ số cách đánh riêng
        @PatchMapping("/{id}/heso")
        public ResponseEntity<Player> updateHeSo (@PathVariable Long id, @RequestParam Double heSo){
            return ResponseEntity.ok(playerService.updateHeSoCachDanh(id, heSo));
        }
//   http://localhost:8080/api/player/10/heso?heSo=2.5

    }


