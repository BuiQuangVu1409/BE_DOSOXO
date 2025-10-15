package com.example.doxoso.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
@Entity
@Table(name = "ket_qua_mien_trung")

public class KetQuaMienTrung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String soTrung;
    private String giai;
    private String tenDai;
    private LocalDate ngay;
    private String thu;

}
