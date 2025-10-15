package com.example.doxoso.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter

public class PlayerDoKetQuaDto {

        private Long playerId;
        private String name;   // đổi từ ten → name
        private Double hoaHong; // đổi từ heSoHoaHong → hoaHong
        private Double heSoCachDanh;
        private List<DoiChieuKetQuaDto> ketQua;


}
