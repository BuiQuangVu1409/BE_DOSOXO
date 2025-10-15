package com.example.doxoso.repository;

import com.example.doxoso.model.Player;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // Lấy danh sách tất cả playerId (nhẹ hơn findAll() nếu bạn chỉ cần ID)
    @Query("select p.id from Player p")
    List<Long> findAllIds();



    List<Player> findByIdIn(Collection<Long> ids);
}
