package com.example.doxoso.service;

import com.example.doxoso.model.Player;

import java.util.List;
import java.util.Optional;

public interface IPlayerService {

    Player createPlayer(Player player);   // thêm mới

    Player updatePlayer(Long id, Player player);   // cập nhật thông tin

    void deletePlayer(Long id);   // xóa player

    Optional<Player> getPlayerById(Long id);   // lấy player theo id

    List<Player> getAllPlayers();   // lấy tất cả players

    Player updateHoaHong(Long id, Double hoaHong);  // cập nhật hoa hồng

    Player updateHeSoCachDanh(Long id, Double heSo); // cập nhật hệ số cách đánh

    // 🔍 THÊM MỚI: tìm player theo tên (để FE search)
    List<Player> searchPlayersByName(String keyword);
}
