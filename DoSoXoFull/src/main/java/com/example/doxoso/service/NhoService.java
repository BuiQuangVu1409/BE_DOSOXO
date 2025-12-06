//package com.example.doxoso.service;
//
//import com.example.doxoso.model.DoiChieuKetQuaDto;
//import com.example.doxoso.repository.KetQuaMienBacRepository;
//import com.example.doxoso.repository.KetQuaMienNamRepository;
//import com.example.doxoso.repository.KetQuaMienTrungRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.text.Normalizer;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//import java.util.regex.Pattern;
//
//@Service
//public class NhoService {
//
//    @Autowired private KetQuaMienBacRepository bacRepo;
//    @Autowired private KetQuaMienTrungRepository trungRepo;
//    @Autowired private KetQuaMienNamRepository namRepo;
//    @Autowired private TinhTienService tinhTienService;
//
//    private static final String CACH_DANH_HIEN_THI = "NHỎ";
//    private static final String CACH_DANH_MA = "NHO";
//
//    public DoiChieuKetQuaDto xuLyNho(Long playerId,
//                                     String soDanh,
//                                     String mien,
//                                     String tenDai,
//                                     LocalDate ngay,
//                                     String tienDanh) {
//        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
//        dto.setSoDanh(soDanh);
//        dto.setMien(mien);
//        dto.setTenDai(tenDai);
//        dto.setNgay(ngay);
//        dto.setCachDanh(CACH_DANH_HIEN_THI);
//        dto.setTienDanh(tienDanh);
//
//        // 1) Validate tiền đánh
//        if (tienDanh == null || tienDanh.isBlank()) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Thiếu tiền đánh"));
//            return dto;
//        }
//
//        // 2) Lấy kết quả Đặc Biệt theo miền/đài/ngày
//        String ketQua = timKetQuaTheoMien(mien, tenDai, ngay);
//
//        // 🔥 MỚI: Validate Giải ĐB theo từng miền
//        // - MB: chấp nhận 5 số
//        // - MT, MN: yêu cầu 6 số
//        if (!isValidGiaiDbForMien(mien, ketQua)) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Giải ĐB không hợp lệ hoặc không tìm thấy cho đài "
//                    + safe(tenDai) + " ngày " + ngay + ": " + ketQua));
//            return dto;
//        }
//
//        // 3) Lấy đuôi 2 số an toàn
//        Integer duoi = duoi2So(ketQua);
//        if (duoi == null) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Không tách được 2 số cuối từ Giải ĐB: " + ketQua));
//            return dto;
//        }
//
//        // 4) Quy tắc TRÚNG NHỎ: đuôi ∈ [0..49]
//        boolean trungNho = duoi >= 0 && duoi <= 49;
//        dto.setTrung(trungNho);
//        dto.setCachTrung(CACH_DANH_HIEN_THI + " – Đuôi: " + duoi);
//
//        if (trungNho) {
//            dto.setGiaiTrung("Giải ĐB");
//            dto.setTienTrung(tinhTienService.tinhTienNho(playerId, CACH_DANH_MA, tienDanh, mien));
//        } else {
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Trật – Đuôi là " + duoi));
//        }
//
//        return dto;
//    }
//
//    /**
//     * Lấy số trúng của Giải ĐẶC BIỆT cho đúng đài/ngày/miền.
//     * - So sánh tên đài theo kiểu bỏ dấu + ignore case.
//     * - Nhận diện nhiều biến thể "Đặc Biệt": ĐẶC BIỆT, ĐB, G.DB, GIAI DAC BIET...
//     * - MB: có fallback nếu filter theo tên đài không thấy.
//     */
//    private String timKetQuaTheoMien(String mien, String tenDai, LocalDate ngay) {
//        String m = removeDiacritics(safe(mien)).toUpperCase();
//        String tenDaiNorm = safe(tenDai);
//
//        if (m.contains("BAC")) {
//            Optional<String> byTenDai = bacRepo.findAllByNgay(ngay).stream()
//                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiDacBiet(k.getGiai()))
//                    .map(k -> k.getSoTrung())
//                    .findFirst();
//            if (byTenDai.isPresent()) return byTenDai.get();
//
//            return bacRepo.findAllByNgay(ngay).stream()
//                    .filter(k -> laGiaiDacBiet(k.getGiai()))
//                    .map(k -> k.getSoTrung())
//                    .findFirst()
//                    .orElse(null);
//
//        } else if (m.contains("TRUNG")) {
//            return trungRepo.findAllByNgay(ngay).stream()
//                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiDacBiet(k.getGiai()))
//                    .map(k -> k.getSoTrung())
//                    .findFirst()
//                    .orElse(null);
//
//        } else if (m.contains("NAM")) {
//            return namRepo.findAllByNgay(ngay).stream()
//                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiDacBiet(k.getGiai()))
//                    .map(k -> k.getSoTrung())
//                    .findFirst()
//                    .orElse(null);
//        }
//
//        return null;
//    }
//
//    /**
//     * Validate Giải ĐB theo từng miền:
//     * - MIỀN BẮC: Giải ĐB hiện tại chỉ còn 5 số
//     * - MIỀN TRUNG / MIỀN NAM: vẫn là 6 số
//     */
//    private boolean isValidGiaiDbForMien(String mien, String ketQua) {
//        if (ketQua == null) return false;
//
//        // Lấy toàn bộ chữ số (phòng trường hợp có space, ký tự khác)
//        String digits = ketQua.replaceAll("\\D", "");
//        if (digits.length() < 2) return false; // < 2 số thì chắc chắn sai, không tách được đuôi
//
//        String m = removeDiacritics(safe(mien)).toUpperCase();
//
//        if (m.contains("BAC")) {
//            // MIỀN BẮC: Giải ĐB 5 số
//            return digits.length() == 5;
//        } else if (m.contains("TRUNG") || m.contains("NAM")) {
//            // MIỀN TRUNG + MIỀN NAM: Giữ 6 số như cũ
//            return digits.length() == 6;
//        }
//
//        // Miền không xác định rõ → coi như không hợp lệ
//        return false;
//    }
//
//    /** Nhận diện "Đặc Biệt" theo nhiều dạng, bỏ dấu/ký tự đặc biệt và so sánh */
//    private boolean laGiaiDacBiet(String giai) {
//        String norm = removeDiacritics(safe(giai)).toUpperCase().replaceAll("[^A-Z0-9]", "");
//        // Hợp lệ: DACBIET, DB, GDB, GIAIDACBIET
//        return norm.equals("DACBIET")
//                || norm.equals("DB")
//                || norm.equals("GDB")
//                || norm.equals("GIAIDACBIET");
//    }
//
//    /** Lấy 2 số cuối an toàn */
//    private Integer duoi2So(String soGiaiDB) {
//        if (soGiaiDB == null) return null;
//        String s = soGiaiDB.replaceAll("\\D", "");
//        if (s.length() < 2) return null;
//        return Integer.parseInt(s.substring(s.length() - 2));
//    }
//
//    /** So sánh 2 chuỗi theo kiểu bỏ dấu + ignore case + trim */
//    private boolean equalsNoAccent(String a, String b) {
//        return removeDiacritics(safe(a)).equalsIgnoreCase(removeDiacritics(safe(b)));
//    }
//
//    /** Bỏ dấu tiếng Việt */
//    private String removeDiacritics(String input) {
//        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
//        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
//                .matcher(normalized)
//                .replaceAll("")
//                .replace('đ', 'd')
//                .replace('Đ', 'D')
//                .trim();
//    }
//
//    /** Tránh NPE */
//    private String safe(String s) {
//        return s == null ? "" : s.trim();
//    }
//}
package com.example.doxoso.service;

import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class NhoService {

    @Autowired private KetQuaMienBacRepository bacRepo;
    @Autowired private KetQuaMienTrungRepository trungRepo;
    @Autowired private KetQuaMienNamRepository namRepo;
    @Autowired private TinhTienService tinhTienService;

    private static final String CACH_DANH_HIEN_THI = "NHỎ";
    private static final String CACH_DANH_MA = "NHO";

    public DoiChieuKetQuaDto xuLyNho(Long playerId,
                                     String soDanh,
                                     String mien,
                                     String tenDai,
                                     LocalDate ngay,
                                     String tienDanh) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(soDanh);
        dto.setMien(mien);
        dto.setTenDai(tenDai);
        dto.setNgay(ngay);
        dto.setCachDanh(CACH_DANH_HIEN_THI);
        dto.setTienDanh(tienDanh);

        // 1) Validate tiền đánh
        if (tienDanh == null || tienDanh.isBlank()) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Thiếu tiền đánh"));
            return dto;
        }

        // 2) Lấy kết quả Đặc Biệt theo miền/đài/ngày
        String ketQua = timKetQuaTheoMien(mien, tenDai, ngay);

        // 🔥 Validate Giải ĐB theo từng miền
        if (!isValidGiaiDbForMien(mien, ketQua)) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Giải ĐB không hợp lệ hoặc không tìm thấy cho đài "
                    + safe(tenDai) + " ngày " + ngay + ": " + ketQua));
            return dto;
        }

        // 3) Lấy đuôi 2 số an toàn
        Integer duoi = duoi2So(ketQua);
        if (duoi == null) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không tách được 2 số cuối từ Giải ĐB: " + ketQua));
            return dto;
        }

        // 4) Quy tắc TRÚNG NHỎ: đuôi ∈ [0..49]
        boolean trungNho = duoi >= 0 && duoi <= 49;
        dto.setTrung(trungNho);
        dto.setCachTrung(CACH_DANH_HIEN_THI + " – Đuôi: " + duoi);

        if (trungNho) {
            dto.setGiaiTrung("Giải ĐB");
            dto.setTienTrung(tinhTienService.tinhTienNho(playerId, CACH_DANH_MA, tienDanh, mien));
        } else {
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Trật – Đuôi là " + duoi));
        }

        return dto;
    }

    /* ===================== MỚI: Chuẩn hóa mã miền ===================== */

    private String toMienCode(String mien) {
        String u = removeDiacritics(safe(mien)).toUpperCase();
        if (u.equals("MB") || u.contains("BAC"))   return "MB";
        if (u.equals("MT") || u.contains("TRUNG")) return "MT";
        if (u.equals("MN") || u.contains("NAM"))   return "MN";
        return "??";
    }

    /**
     * Lấy số trúng của Giải ĐẶC BIỆT cho đúng đài/ngày/miền.
     */
    private String timKetQuaTheoMien(String mien, String tenDai, LocalDate ngay) {
        String code = toMienCode(mien);
        String tenDaiNorm = safe(tenDai);

        if ("MB".equals(code)) {
            Optional<String> byTenDai = bacRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiDacBiet(k.getGiai()))
                    .map(k -> k.getSoTrung())
                    .findFirst();
            if (byTenDai.isPresent()) return byTenDai.get();

            return bacRepo.findAllByNgay(ngay).stream()
                    .filter(k -> laGiaiDacBiet(k.getGiai()))
                    .map(k -> k.getSoTrung())
                    .findFirst()
                    .orElse(null);

        } else if ("MT".equals(code)) {
            return trungRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiDacBiet(k.getGiai()))
                    .map(k -> k.getSoTrung())
                    .findFirst()
                    .orElse(null);

        } else if ("MN".equals(code)) {
            return namRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiDacBiet(k.getGiai()))
                    .map(k -> k.getSoTrung())
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    /**
     * Validate Giải ĐB theo từng miền:
     * - MB: Giải ĐB 5 số
     * - MT / MN: Giải ĐB 6 số
     */
    private boolean isValidGiaiDbForMien(String mien, String ketQua) {
        if (ketQua == null) return false;

        String digits = ketQua.replaceAll("\\D", "");
        if (digits.length() < 2) return false;

        String code = toMienCode(mien);

        return switch (code) {
            case "MB" -> digits.length() == 5;
            case "MT", "MN" -> digits.length() == 6;
            default -> false;
        };
    }

    /** Nhận diện "Đặc Biệt" theo nhiều dạng, bỏ dấu/ký tự đặc biệt và so sánh */
    private boolean laGiaiDacBiet(String giai) {
        String norm = removeDiacritics(safe(giai)).toUpperCase().replaceAll("[^A-Z0-9]", "");
        // Hợp lệ: DACBIET, DB, GDB, GIAIDACBIET
        return norm.equals("DACBIET")
                || norm.equals("DB")
                || norm.equals("GDB")
                || norm.equals("GIAIDACBIET");
    }

    /** Lấy 2 số cuối an toàn */
    private Integer duoi2So(String soGiaiDB) {
        if (soGiaiDB == null) return null;
        String s = soGiaiDB.replaceAll("\\D", "");
        if (s.length() < 2) return null;
        return Integer.parseInt(s.substring(s.length() - 2));
    }

    /** So sánh 2 chuỗi theo kiểu bỏ dấu + ignore case + trim */
    private boolean equalsNoAccent(String a, String b) {
        return removeDiacritics(safe(a)).equalsIgnoreCase(removeDiacritics(safe(b)));
    }

    /** Bỏ dấu tiếng Việt */
    private String removeDiacritics(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .trim();
    }

    /** Tránh NPE */
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
