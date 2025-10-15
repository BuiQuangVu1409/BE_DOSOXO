package com.example.doxoso.repository;

import com.example.doxoso.model.KetQuaMienTrung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface KetQuaMienTrungRepository extends JpaRepository<KetQuaMienTrung, Long> {

    // Lấy toàn bộ kết quả theo ngày
    List<KetQuaMienTrung> findAllByNgay(LocalDate ngay);

}
