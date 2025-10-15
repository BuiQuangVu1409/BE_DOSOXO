package com.example.doxoso.service;
import com.example.doxoso.model.KetQuaMienBac;
import com.example.doxoso.model.KetQuaMienNam;
import com.example.doxoso.model.KetQuaMienTrung;
import jakarta.persistence.PrePersist;
import org.springframework.stereotype.Service;


import java.time.DayOfWeek;
import java.time.LocalDate;
@Service
public class UpdateThu {

        @PrePersist
        public void setThu(Object entity) {
            if (entity instanceof KetQuaMienBac) {
                KetQuaMienBac obj = (KetQuaMienBac) entity;
                obj.setThu(chuyenDoiThu(obj.getNgay()));
            } else if (entity instanceof KetQuaMienTrung) {
                KetQuaMienTrung obj = (KetQuaMienTrung) entity;
                obj.setThu(chuyenDoiThu(obj.getNgay()));
            } else if (entity instanceof KetQuaMienNam) {
                KetQuaMienNam obj = (KetQuaMienNam) entity;
                obj.setThu(chuyenDoiThu(obj.getNgay()));
            }
        }
        private String chuyenDoiThu(LocalDate date) {
            if (date == null) return "";
            DayOfWeek day = date.getDayOfWeek();
            switch (day) {
                case MONDAY: return "Thứ Hai";
                case TUESDAY: return "Thứ Ba";
                case WEDNESDAY: return "Thứ Tư";
                case THURSDAY: return "Thứ Năm";
                case FRIDAY: return "Thứ Sáu";
                case SATURDAY: return "Thứ Bảy";
                case SUNDAY: return "Chủ Nhật";
                default: return "";
            }
        }
    }


