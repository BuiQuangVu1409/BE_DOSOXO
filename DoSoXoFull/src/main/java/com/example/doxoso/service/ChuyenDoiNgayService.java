package com.example.doxoso.service;


import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
@Service
public class ChuyenDoiNgayService {
    
        public String chuyenDoiThu(LocalDate date) {
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
