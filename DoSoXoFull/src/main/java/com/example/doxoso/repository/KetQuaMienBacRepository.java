package com.example.doxoso.repository;

import com.example.doxoso.model.KetQuaMienBac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository

public interface KetQuaMienBacRepository extends JpaRepository<KetQuaMienBac, Long> {
    List<KetQuaMienBac> findAllByNgay(LocalDate ngay);



}
