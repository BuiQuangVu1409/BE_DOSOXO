package com.example.doxoso.repository;

import com.example.doxoso.model.Bet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository // Đánh dấu đây là bean Repository của Spring
public interface BetRepository extends JpaRepository<Bet, Long> { // Kế thừa CRUD chuẩn cho Bet, khóa chính Long

        // ================== TỐI ƯU TẢI DỮ LIỆU (TRÁNH N+1) ==================

        @Override
        @EntityGraph(attributePaths = "player") // Luôn JOIN FETCH player khi gọi findAll()
        List<Bet> findAll();


        @EntityGraph(attributePaths = "player")
        @Query("select b from Bet b")
        List<Bet> findAllWithPlayer();

        // ================== TRUY VẤN THEO QUAN HỆ (player.id) ==================

        // Chuẩn nhất khi entity có field "player": truy vết thuộc tính lồng bằng "Player_Id"
        List<Bet> findByPlayer_Id(Long playerId);

        // Alias cho code cũ (nếu trước đây gọi findByPlayerId). Gọi lại method chính ở trên.
        default List<Bet> findByPlayerId(Long playerId) {
                return findByPlayer_Id(playerId);
        }

        // Lấy theo player trong khoảng ngày, kèm fetch player để giảm N+1
        @EntityGraph(attributePaths = "player")
        List<Bet> findByPlayer_IdAndNgayBetween(Long playerId, LocalDate from, LocalDate to);

        // Alias theo tên cũ
        default List<Bet> findByPlayerIdAndNgayBetween(Long playerId, LocalDate from, LocalDate to) {
                return findByPlayer_IdAndNgayBetween(playerId, from, to);
        }

        // ================== TRUY VẤN THEO THỜI GIAN ==================

        // Lấy tất cả bet trong một khoảng ngày (không lọc theo player), kèm fetch player
        @EntityGraph(attributePaths = "player")
        List<Bet> findByNgayBetween(LocalDate from, LocalDate to);

        // Lấy đúng 1 ngày theo player
        List<Bet> findByPlayer_IdAndNgay(Long playerId, LocalDate ngay);

        // Alias theo tên cũ
        default List<Bet> findByPlayerIdAndNgay(Long playerId, LocalDate ngay) {
                return findByPlayer_IdAndNgay(playerId, ngay);
        }

        // Lấy tất cả bet đúng 1 ngày (mọi player)
        List<Bet> findByNgay(LocalDate ngay);

        // ================== TRUY VẤN KẾT HỢP BỘ LỌC CHUỖI ==================

        // Tìm theo player + bộ lọc miền/đài (LIKE, không phân biệt hoa thường)
        List<Bet> findByPlayer_IdAndMienContainingIgnoreCaseAndDaiContainingIgnoreCase(
                Long playerId, String mien, String dai
        );

        // Alias theo tên cũ
        default List<Bet> findByPlayerIdAndMienContainingIgnoreCaseAndDaiContainingIgnoreCase(
                Long playerId, String mien, String dai
        ) {
                return findByPlayer_IdAndMienContainingIgnoreCaseAndDaiContainingIgnoreCase(playerId, mien, dai);
        }

        // ================== TRUY VẤN TÙY CHỈNH JPQL ==================

        // Lấy danh sách playerId duy nhất trong 1 ngày (nhẹ hơn việc tải full Bet rồi map)
        @Query("select distinct b.player.id from Bet b where b.ngay = :ngay")
        List<Long> findDistinctPlayerIdsByNgay(@Param("ngay") LocalDate ngay);
}
