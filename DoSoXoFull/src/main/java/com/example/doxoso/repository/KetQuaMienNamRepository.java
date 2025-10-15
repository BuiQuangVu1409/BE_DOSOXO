package com.example.doxoso.repository;

import com.example.doxoso.model.KetQuaMienNam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface KetQuaMienNamRepository extends JpaRepository<KetQuaMienNam, Long> {
    List<KetQuaMienNam> findAllByNgay(LocalDate ngay);


}
