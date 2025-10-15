package com.example.doxoso.repository;

import com.example.doxoso.model.KetQuaNguoiChoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface KetQuaNguoiChoiRepository extends JpaRepository<KetQuaNguoiChoi, Long> {





    // Lấy theo playerId
    List<KetQuaNguoiChoi> findByPlayerId(Long playerId);

    // Lấy theo playerName
    List<KetQuaNguoiChoi> findByPlayerName(String playerName);

    // Lấy theo ngày chơi
    List<KetQuaNguoiChoi> findByNgayChoi(LocalDate ngayChoi);

    // Kết hợp playerId + ngày
    List<KetQuaNguoiChoi> findByPlayerIdAndNgayChoi(Long playerId, LocalDate ngayChoi);

    // Nếu muốn kết hợp playerName + ngày chơi
    List<KetQuaNguoiChoi> findByPlayerNameAndNgayChoi(String playerName, LocalDate ngayChoi);

    // lấy tất cả kết quả theo khoảng ngày


        // các method khác ...

        @Query("SELECT k FROM KetQuaNguoiChoi k " +
                "WHERE k.ngayChoi BETWEEN :startDate AND :endDate")
        List<KetQuaNguoiChoi> findByNgayChoiTuNgay(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    List<com.example.doxoso.model.KetQuaNguoiChoi> findByNgayChoiAndTrungTrue(LocalDate ngay);
    // 👉 Thêm cái này để lọc theo danh sách miền (ví dụ ["MB","MN"])
    List<KetQuaNguoiChoi> findByNgayChoiAndTrungTrueAndMienIn(LocalDate ngay, Collection<String> miens);




    // NEW: dùng để kiểm tra một "tin" (SoNguoiChoi.id) đã được lưu kết quả chưa
    boolean existsBySourceSoId(Long sourceSoId);
    }




