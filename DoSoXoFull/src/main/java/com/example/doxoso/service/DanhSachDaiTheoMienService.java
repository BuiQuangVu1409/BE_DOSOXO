package com.example.doxoso.service;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
@Service
public class DanhSachDaiTheoMienService {
        private static final Map<String, List<String>> LICH_MIEN_NAM = Map.of(
                "Thứ Hai", List.of("CÀ MAU", "ĐỒNG THÁP", "TP.HỒ CHÍ MINH"),
                "Thứ Ba", List.of("BẠC LIÊU", "BẾN TRE", "VŨNG TÀU"),
                "Thứ Tư", List.of("CẦN THƠ", "ĐỒNG NAI", "SÓC TRĂNG"),
                "Thứ Năm", List.of("TÂY NINH", "AN GIANG", "BÌNH THUẬN"),
                "Thứ Sáu", List.of("VĨNH LONG", "BÌNH DƯƠNG", "TRÀ VINH"),
                "Thứ Bảy", List.of("TP.HỒ CHÍ MINH", "LONG AN", "BÌNH PHƯỚC", "HẬU GIANG"),
                "Chủ Nhật", List.of("TIỀN GIANG", "KIÊN GIANG", "ĐÀ LẠT")
        );

        private static final Map<String, List<String>> LICH_MIEN_TRUNG = Map.of(
                "Thứ Hai", List.of("PHÚ YÊN", "THỪA THIÊN HUẾ"),
                "Thứ Ba", List.of("QUẢNG NAM", "ĐẮK LẮK"),
                "Thứ Tư", List.of("KHÁNH HÒA", "ĐÀ NẴNG"),
                "Thứ Năm", List.of("BÌNH ĐỊNH", "QUẢNG BÌNH", "QUẢNG TRỊ"),
                "Thứ Sáu", List.of("GIA LAI", "NINH THUẬN"),
                "Thứ Bảy", List.of("ĐẮK NÔNG", "QUẢNG NGÃI"),
                "Chủ Nhật", List.of("KON TUM", "KHÁNH HÒA")
        );

        private static final Map<String, List<String>> LICH_MIEN_BAC = Map.of(

                "Thứ Hai", List.of("HÀ NỘI"),
                "Thứ Ba", List.of("HÀ NỘI"),
                "Thứ Tư", List.of("HÀ NỘI"),
                "Thứ Năm", List.of("HÀ NỘI"),
                "Thứ Sáu", List.of("HÀ NỘI"),
                "Thứ Bảy", List.of("HÀ NỘI"),
                "Chủ Nhật", List.of("HÀ NỘI")
        );

        public List<String> layDanhSachDaiTheoThuVaMien(String thu, String mien) {
            if (thu == null || mien == null) return List.of();

            switch (mien.toUpperCase()) {
                case "MIỀN NAM":
                    return LICH_MIEN_NAM.getOrDefault(thu, List.of());
                case "MIỀN TRUNG":
                    return LICH_MIEN_TRUNG.getOrDefault(thu, List.of());
                case "MIỀN BẮC":
                    return LICH_MIEN_BAC.getOrDefault(thu, List.of());
                default:
                    return List.of();
            }
        }

        public boolean isDaiMoThuong(String tenDai, String thu, String mien) {
            List<String> danhSach = layDanhSachDaiTheoThuVaMien(thu, mien);
            return danhSach.stream().anyMatch(d -> d.equalsIgnoreCase(tenDai));
        }
    public List<String> layDanhSachDaiTheoSoLuong(String mien, int soLuong, LocalDate ngay, ChuyenDoiNgayService chuyenDoiNgayService) {
        String thu = chuyenDoiNgayService.chuyenDoiThu(ngay);
        List<String> danhSachDai = layDanhSachDaiTheoThuVaMien(thu, mien);
        if (soLuong <= 0 || soLuong > danhSachDai.size()) return danhSachDai;
        return danhSachDai.subList(0, soLuong);
    }

}

