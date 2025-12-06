//package com.example.doxoso.repository;
//
//import com.example.doxoso.model.KetQuaNguoiChoi;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDate;
//import java.util.Collection;
//import java.util.List;
//
//@Repository
//public interface KetQuaNguoiChoiRepository extends JpaRepository<KetQuaNguoiChoi, Long> {
//
//    // ================== CRUD CƠ BẢN (đang được service dùng) ==================
//
//    // Lấy theo playerId
//    List<KetQuaNguoiChoi> findByPlayerId(Long playerId);
//
//    // Lấy theo playerName
//    List<KetQuaNguoiChoi> findByPlayerName(String playerName);
//
//    // Lấy theo ngày chơi
//    List<KetQuaNguoiChoi> findByNgayChoi(LocalDate ngayChoi);
//
//    // Kết hợp playerId + ngày
//    List<KetQuaNguoiChoi> findByPlayerIdAndNgayChoi(Long playerId, LocalDate ngayChoi);
//
//    // Kết hợp playerName + ngày
//    List<KetQuaNguoiChoi> findByPlayerNameAndNgayChoi(String playerName, LocalDate ngayChoi);
//
//    // Lấy theo khoảng ngày (dùng cho getKetQuaTrongKhoang)
//    @Query("SELECT k FROM KetQuaNguoiChoi k " +
//            "WHERE k.ngayChoi BETWEEN :startDate AND :endDate")
//    List<KetQuaNguoiChoi> findByNgayChoiTuNgay(
//            @Param("startDate") LocalDate startDate,
//            @Param("endDate")   LocalDate endDate
//    );
//
//    // ================== PHẦN DÙNG CHO KẾT QUẢ TỊCH / THỐNG KÊ ==================
//
//    // Theo ngày + chỉ lấy bản ghi TRÚNG
//    List<KetQuaNguoiChoi> findByNgayChoiAndTrungTrue(LocalDate ngayChoi);
//
//    // Theo ngày + TRÚNG + lọc theo danh sách miền (["MIỀN BẮC","MIỀN TRUNG","MIỀN NAM"])
//    List<KetQuaNguoiChoi> findByNgayChoiAndTrungTrueAndMienIn(LocalDate ngayChoi, Collection<String> miens);
//
//    // Giữ lại API cũ: playerId + ngày + trúng (không phân biệt summary/chi tiết)
//    List<KetQuaNguoiChoi> findByPlayerIdAndNgayChoiAndTrungTrue(Long playerId, LocalDate ngayChoi);
//
//    // Xóa toàn bộ KQ chi tiết của 1 BET (theo sourceSoId = bet.id)
//    void deleteBySourceSoId(Long sourceSoId);
//
//    // NEW: Chi tiết trúng (per-đài) của 1 player trong 1 ngày, chỉ lấy bản ghi chi tiết (summary = false)
//    @Query("SELECT k FROM KetQuaNguoiChoi k " +
//            "WHERE k.playerId = :playerId " +
//            "AND k.ngayChoi = :ngayChoi " +
//            "AND k.trung = TRUE " +
//            "AND k.summary = FALSE")
//    List<KetQuaNguoiChoi> findChiTietTrungByPlayerAndNgay(
//            @Param("playerId") Long playerId,
//            @Param("ngayChoi") LocalDate ngayChoi
//    );
//
//    // Dùng trong KetQuaNguoiChoiService.luuKetQua:
//    // kiểm tra 1 tin (Bet.id) đã được lưu kết quả hay chưa
//    boolean existsBySourceSoId(Long sourceSoId);
//}
package com.example.doxoso.repository;

import com.example.doxoso.model.KetQuaNguoiChoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface KetQuaNguoiChoiRepository extends JpaRepository<KetQuaNguoiChoi, Long> {

    // ================== CRUD CƠ BẢN (đang được service dùng) ==================

    // Lấy theo playerId
    List<KetQuaNguoiChoi> findByPlayerId(Long playerId);

    // Lấy theo playerName
    List<KetQuaNguoiChoi> findByPlayerName(String playerName);

    // Lấy theo ngày chơi
    List<KetQuaNguoiChoi> findByNgayChoi(LocalDate ngayChoi);

    // Kết hợp playerId + ngày
    List<KetQuaNguoiChoi> findByPlayerIdAndNgayChoi(Long playerId, LocalDate ngayChoi);

    // Kết hợp playerName + ngày
    List<KetQuaNguoiChoi> findByPlayerNameAndNgayChoi(String playerName, LocalDate ngayChoi);

    // Lấy theo khoảng ngày (dùng cho getKetQuaTrongKhoang)
    @Query("SELECT k FROM KetQuaNguoiChoi k " +
            "WHERE k.ngayChoi BETWEEN :startDate AND :endDate")
    List<KetQuaNguoiChoi> findByNgayChoiTuNgay(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

    // ================== PHẦN DÙNG CHO KẾT QUẢ TỊCH / THỐNG KÊ ==================

    // Theo ngày + chỉ lấy bản ghi TRÚNG
    List<KetQuaNguoiChoi> findByNgayChoiAndTrungTrue(LocalDate ngayChoi);

    // Theo ngày + TRÚNG + lọc theo danh sách miền (["MIỀN BẮC","MIỀN TRUNG","MIỀN NAM"])
    List<KetQuaNguoiChoi> findByNgayChoiAndTrungTrueAndMienIn(LocalDate ngayChoi, Collection<String> miens);

    // Giữ lại API cũ: playerId + ngày + trúng (không phân biệt summary/chi tiết)
    List<KetQuaNguoiChoi> findByPlayerIdAndNgayChoiAndTrungTrue(Long playerId, LocalDate ngayChoi);

    // Xóa toàn bộ KQ chi tiết của 1 BET (theo sourceSoId = bet.id)
    void deleteBySourceSoId(Long sourceSoId);

    // NEW: Chi tiết trúng (per-đài) của 1 player trong 1 ngày, chỉ lấy bản ghi chi tiết (summary = false)
    @Query("SELECT k FROM KetQuaNguoiChoi k " +
            "WHERE k.playerId = :playerId " +
            "AND k.ngayChoi = :ngayChoi " +
            "AND k.trung = TRUE " +
            "AND k.summary = FALSE")
    List<KetQuaNguoiChoi> findChiTietTrungByPlayerAndNgay(
            @Param("playerId") Long playerId,
            @Param("ngayChoi") LocalDate ngayChoi
    );

    // Dùng trong KetQuaNguoiChoiService.luuKetQua:
    // kiểm tra 1 tin (Bet.id) đã được lưu kết quả hay chưa
    boolean existsBySourceSoId(Long sourceSoId);

    // 🔥 NEW: XÓA toàn bộ KQ (summary + chi tiết) của 1 player trong 1 ngày
    @Modifying
    @Transactional
    @Query("DELETE FROM KetQuaNguoiChoi k " +
            "WHERE k.playerId = :playerId AND k.ngayChoi = :ngayChoi")
    int deleteByPlayerIdAndNgayChoi(@Param("playerId") Long playerId,
                                    @Param("ngayChoi") LocalDate ngayChoi);
}
