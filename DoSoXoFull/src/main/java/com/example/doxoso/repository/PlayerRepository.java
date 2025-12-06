package com.example.doxoso.repository;

import com.example.doxoso.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // Lấy danh sách tất cả playerId (nhẹ hơn findAll() nếu bạn chỉ cần ID)
    @Query("select p.id from Player p")
    List<Long> findAllIds();

    List<Player> findByIdIn(Collection<Long> ids);

    // 🔍 Tìm player theo tên (field name trong entity Player)
    // KHÔNG phân biệt hoa/thường, dùng chứa chuỗi (LIKE %keyword%)
    List<Player> findByNameContainingIgnoreCase(String keyword);

    // Nếu trong entity Player field tên là khác (vd: playerName) thì đổi lại:
    // List<Player> findByPlayerNameContainingIgnoreCase(String keyword);
}
