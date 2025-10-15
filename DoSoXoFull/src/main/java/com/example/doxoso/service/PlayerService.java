package com.example.doxoso.service;

import com.example.doxoso.model.Player;
import com.example.doxoso.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <—
import java.util.List;
import java.util.Optional;

@Service
public class PlayerService implements IPlayerService {

    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // ========= CREATE =========
    @Override
    @Transactional
    public Player createPlayer(Player player) {
        // Ép tạo mới: KHÔNG merge
        player.setId(null);
        // Nếu có @Version trong Player:
        // player.setVersion(null);

        normalizeForSave(player);
        return playerRepository.save(player); // persist
    }

    // Cho controller/route đang gọi addPlayer — chuyển hướng về create
    @Transactional
    public Player addPlayer(Player player) {
        // BỎ check existsById(...) để tránh vô tình merge với id client gửi lên
        player.setId(null);
        // player.setVersion(null);
        normalizeForSave(player);
        return playerRepository.save(player);
    }

    // ========= UPDATE (PATCH toàn phần theo quy ước) =========
    @Override
    @Transactional
    public Player updatePlayer(Long id, Player patch) {
        return playerRepository.findById(id)
                .map(p -> {
                    // chỉ copy các field cho phép sửa
                    p.setName(trimOrNull(patch.getName()));
                    p.setPhone(patch.getPhone());
                    p.setHoaHong(patch.getHoaHong());
                    p.setHeSoCachDanh(patch.getHeSoCachDanh());
                    return playerRepository.save(p);
                })
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
    }

    // ========= REPLACE (PUT) =========
    @Transactional
    public Player replacePlayer(Long id, Player incoming) {
        return playerRepository.findById(id)
                .map(existing -> {
                    // Không nhận version từ client để tránh stale
                    existing.setName(trimOrNull(incoming.getName()));
                    existing.setPhone(incoming.getPhone());
                    existing.setHoaHong(incoming.getHoaHong());
                    existing.setHeSoCachDanh(incoming.getHeSoCachDanh());
                    return playerRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
    }

    // ========= PATCH HOA HỒNG =========
    @Override
    @Transactional
    public Player updateHoaHong(Long id, Double hoaHong) {
        return playerRepository.findById(id)
                .map(p -> {
                    p.setHoaHong(hoaHong);
                    return playerRepository.save(p);
                })
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
    }

    // ========= PATCH HỆ SỐ =========
    @Override
    @Transactional
    public Player updateHeSoCachDanh(Long id, Double heSo) {
        return playerRepository.findById(id)
                .map(p -> {
                    p.setHeSoCachDanh(heSo);
                    return playerRepository.save(p);
                })
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
    }

    // ========= DELETE & READ =========
    @Override
    @Transactional
    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new RuntimeException("Player not found with id: " + id);
        }
        playerRepository.deleteById(id);
    }

    @Override
    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }

    @Override
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    // ===== Helpers =====
    private void normalizeForSave(Player p) {
        p.setName(trimOrNull(p.getName()));
        // có thể bổ sung chuẩn hóa khác nếu cần
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
