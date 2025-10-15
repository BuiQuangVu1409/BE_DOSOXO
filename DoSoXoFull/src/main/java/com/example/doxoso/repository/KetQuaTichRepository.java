package com.example.doxoso.repository;


import com.example.doxoso.model.KetQuaTich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KetQuaTichRepository extends JpaRepository<KetQuaTich, Long> {

    Optional<KetQuaTich> findByPlayerIdAndNgayAndMienCode(Long playerId, LocalDate ngay, String mienCode);

    List<KetQuaTich> findByPlayerIdAndNgay(Long playerId, LocalDate ngay);

    // Nếu bạn vẫn muốn “xóa theo ngày” (ít dùng sau khi upsert):
    @Modifying  // rất quan trọng
    @Query("DELETE FROM KetQuaTich k WHERE k.playerId = :playerId AND k.ngay = :ngay")
    int deleteByPlayerIdAndNgay(@Param("playerId") Long playerId, @Param("ngay") LocalDate ngay);
}

