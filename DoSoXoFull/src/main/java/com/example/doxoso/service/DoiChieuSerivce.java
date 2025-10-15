package com.example.doxoso.service;
import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.Bet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DoiChieuSerivce {
        @Autowired
        private ChuyenDoiNgayService chuyenDoiNgayService;

        public DoiChieuKetQuaDto taoDto(Bet bet) {
            DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
            dto.setSoDanh(bet.getSoDanh());
            dto.setTenDai(bet.getDai());
            dto.setMien(bet.getMien());
            dto.setNgay(bet.getNgay());
            dto.setThu(chuyenDoiNgayService.chuyenDoiThu(bet.getNgay()));
            dto.setCachDanh(bet.getCachDanh());
            dto.setTienDanh(bet.getSoTien());
            return dto;
        }
    }


