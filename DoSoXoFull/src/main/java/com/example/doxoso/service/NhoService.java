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

    // Mã chung cho "NHỎ" bên TinhTienService (giữ như cũ để không phá code)
    private static final String CACH_DANH_MA_CHUNG = "NHO";

    // Các loại "NHỎ" chi tiết
    private static final String LOAI_NHO_DAU       = "NHO_DAU";
    private static final String LOAI_NHO_DUOI      = "NHO_DUOI";
    private static final String LOAI_NHO_DAU_DUOI  = "NHO_DAU_DUOI";

    /**
     * @param cachDanhInput: "NHỎ ĐẦU" | "NHỎ ĐUÔI" | "NHỎ ĐẦU ĐUÔI"
     */
    public DoiChieuKetQuaDto xuLyNho(Long playerId,
                                     String cachDanhInput,
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
        dto.setTienDanh(tienDanh);

        // 0) Chuẩn hóa loại "NHỎ" người chơi chọn
        String loaiMa = parseLoaiNho(cachDanhInput);
        if (loaiMa == null) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of(
                    "Cách đánh không hợp lệ. Chỉ chấp nhận: NHỎ ĐẦU / NHỎ ĐUÔI / NHỎ ĐẦU ĐUÔI"
            ));
            return dto;
        }
        dto.setCachDanh(hienThiTuMa(loaiMa)); // "NHỎ ĐẦU" | "NHỎ ĐUÔI" | "NHỎ ĐẦU ĐUÔI"

        // 1) Validate tiền đánh
        if (tienDanh == null || tienDanh.isBlank()) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Thiếu tiền đánh"));
            return dto;
        }

        String codeMien = toMienCode(mien);

        // 1.1) Chặn các cách đánh không áp dụng cho miền Bắc
        if ("MB".equals(codeMien)
                && (LOAI_NHO_DAU.equals(loaiMa) || LOAI_NHO_DAU_DUOI.equals(loaiMa))) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Cách đánh " + dto.getCachDanh() + " không áp dụng cho Miền Bắc"));
            return dto;
        }

        // 2) Lấy kết quả Giải ĐẶC BIỆT (dùng cho NHỎ ĐUÔI / NHỎ ĐẦU ĐUÔI)
        String ketQuaDb = timKetQuaTheoMien(mien, tenDai, ngay);

        if (!isValidGiaiDbForMien(mien, ketQuaDb)) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of(
                    "Giải ĐB không hợp lệ hoặc không tìm thấy cho đài "
                            + safe(tenDai) + " ngày " + ngay + ": " + ketQuaDb
            ));
            return dto;
        }

        Integer duoiDb = duoi2So(ketQuaDb);
        if (duoiDb == null) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không tách được 2 số cuối từ Giải ĐB: " + ketQuaDb));
            return dto;
        }
        boolean nhoDuoi = duoiDb >= 0 && duoiDb <= 49;   // 00–49 là NHỎ ĐUÔI

        // 3) Nếu là loại có "ĐẦU" (NHO_DAU / NHO_DAU_DUOI) và là MT/MN → lấy Giải 8
        Integer duoiG8 = null;
        boolean nhoDau = false;
        if (!"MB".equals(codeMien) &&
                (LOAI_NHO_DAU.equals(loaiMa) || LOAI_NHO_DAU_DUOI.equals(loaiMa))) {

            duoiG8 = duoi2SoGiaiTam(mien, tenDai, ngay);
            if (duoiG8 == null) {
                dto.setTrung(false);
                dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of(
                        "Không tìm thấy Giải 8 cho đài " + safe(tenDai) + " ngày " + ngay
                ));
                return dto;
            }
            nhoDau = duoiG8 >= 0 && duoiG8 <= 49;      // 00–49 là NHỎ ĐẦU (Giải 8)
        }

        // 4) Áp dụng logic trúng theo từng loại
        switch (loaiMa) {
            case LOAI_NHO_DUOI -> {
                // ===== NHỎ ĐUÔI: MB + MT + MN, dựa vào Giải ĐB =====
                if (nhoDuoi) {
                    dto.setTrung(true);
                    dto.setCachTrung("NHỎ ĐUÔI – Đuôi Giải ĐB: " + String.format("%02d", duoiDb));
                    dto.setGiaiTrung("Giải ĐB");
                    dto.setTienTrung(tinhTienService.tinhTienNho(
                            playerId, CACH_DANH_MA_CHUNG, tienDanh, mien
                    ));
                } else {
                    dto.setTrung(false);
                    dto.setTienTrung(0.0);
                    dto.setSaiLyDo(List.of(
                            "Trật NHỎ ĐUÔI – Đuôi Giải ĐB là " + String.format("%02d", duoiDb)
                    ));
                }
            }
            case LOAI_NHO_DAU -> {
                // ===== NHỎ ĐẦU: chỉ MT/MN, dựa vào Giải 8 =====
                if (nhoDau) {
                    dto.setTrung(true);
                    dto.setCachTrung("NHỎ ĐẦU – 2 số cuối Giải 8: " + String.format("%02d", duoiG8));
                    dto.setGiaiTrung("Giải 8");
                    dto.setTienTrung(tinhTienService.tinhTienNho(
                            playerId, CACH_DANH_MA_CHUNG, tienDanh, mien
                    ));
                } else {
                    dto.setTrung(false);
                    dto.setTienTrung(0.0);
                    dto.setSaiLyDo(List.of(
                            "Trật NHỎ ĐẦU – 2 số cuối Giải 8 là " + String.format("%02d", duoiG8)
                    ));
                }
            }
            case LOAI_NHO_DAU_DUOI -> {
                // ===== NHỎ ĐẦU ĐUÔI: chỉ MT/MN, phải NHỎ cả ĐẦU (G8) lẫn ĐUÔI (ĐB) =====
                if (nhoDuoi && nhoDau) {
                    dto.setTrung(true);
                    dto.setCachTrung(
                            "NHỎ ĐẦU ĐUÔI – Giải 8: " + String.format("%02d", duoiG8)
                                    + ", Giải ĐB: " + String.format("%02d", duoiDb)
                    );
                    dto.setGiaiTrung("Giải 8 + Giải ĐB");
                    dto.setTienTrung(tinhTienService.tinhTienNho(
                            playerId, CACH_DANH_MA_CHUNG, tienDanh, mien
                    ));
                } else {
                    dto.setTrung(false);
                    dto.setTienTrung(0.0);
                    dto.setSaiLyDo(List.of(
                            "Trật NHỎ ĐẦU ĐUÔI – Giải 8: " + String.format("%02d", duoiG8)
                                    + ", Giải ĐB: " + String.format("%02d", duoiDb)
                    ));
                }
            }
            default -> {
                // Không bao giờ vào đây do đã check loaiMa ở trên,
                // nhưng để phòng hờ:
                dto.setTrung(false);
                dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of("Loại NHỎ không xác định"));
            }
        }

        return dto;
    }

    /* ===================== CHUẨN HÓA & TIỆN ÍCH ===================== */

    // Chuẩn hóa miền về "MB" | "MT" | "MN"
    private String toMienCode(String mien) {
        String u = removeDiacritics(safe(mien)).toUpperCase();
        if (u.equals("MB") || u.contains("BAC"))   return "MB";
        if (u.equals("MT") || u.contains("TRUNG")) return "MT";
        if (u.equals("MN") || u.contains("NAM"))   return "MN";
        return "??";
    }

    // Parse "NHỎ ĐẦU", "nhỏ đuôi", "NHO_DAU_DUOI" -> mã nội bộ
    private String parseLoaiNho(String input) {
        String u = removeDiacritics(safe(input)).toUpperCase(); // "NHO DAU", "NHO_DAU", "NHỎ ĐẦU" -> NHO DAU...

        if (u.contains("DAU") && u.contains("DUOI")) {
            return LOAI_NHO_DAU_DUOI;
        }
        if (u.contains("DAU")) {
            return LOAI_NHO_DAU;
        }
        if (u.contains("DUOI")) {
            return LOAI_NHO_DUOI;
        }
        return null;
    }

    // Hiển thị đẹp cho DTO
    private String hienThiTuMa(String loaiMa) {
        return switch (loaiMa) {
            case LOAI_NHO_DAU       -> "NHỎ ĐẦU";
            case LOAI_NHO_DUOI      -> "NHỎ ĐUÔI";
            case LOAI_NHO_DAU_DUOI  -> "NHỎ ĐẦU ĐUÔI";
            default -> "NHỎ";
        };
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
     * Lấy đuôi 2 số của Giải 8 cho MT/MN (NHỎ ĐẦU).
     * - Miền Bắc không có Giải 8 → hàm ở trên đã chặn không cho chơi loại này.
     */
    private Integer duoi2SoGiaiTam(String mien, String tenDai, LocalDate ngay) {
        String code = toMienCode(mien);
        String tenDaiNorm = safe(tenDai);

        if ("MT".equals(code)) {
            return trungRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiTam(k.getGiai()))
                    .map(k -> duoi2So(k.getSoTrung()))
                    .filter(d -> d != null)
                    .findFirst()
                    .orElse(null);
        } else if ("MN".equals(code)) {
            return namRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiTam(k.getGiai()))
                    .map(k -> duoi2So(k.getSoTrung()))
                    .filter(d -> d != null)
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
        String norm = removeDiacritics(safe(giai))
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");
        // Hợp lệ: DACBIET, DB, GDB, GIAIDACBIET
        return norm.equals("DACBIET")
                || norm.equals("DB")
                || norm.equals("GDB")
                || norm.equals("GIAIDACBIET");
    }

    /** Nhận diện "Giải 8" (G8) theo nhiều dạng, bỏ dấu + ký tự đặc biệt */
    private boolean laGiaiTam(String giai) {
        String norm = removeDiacritics(safe(giai))
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");
        // Chấp nhận các dạng phổ biến: G8, GIAITAM, GIATAM
        return norm.equals("G8")
                || norm.equals("GIAITAM")
                || norm.equals("GIATAM");
    }

    /** Lấy 2 số cuối an toàn */
    private Integer duoi2So(String soGiai) {
        if (soGiai == null) return null;
        String s = soGiai.replaceAll("\\D", "");
        if (s.length() < 2) return null;
        return Integer.parseInt(s.substring(s.length() - 2));
    }

    /** So sánh 2 chuỗi theo kiểu bỏ dấu + ignore case + trim */
    private boolean equalsNoAccent(String a, String b) {
        return removeDiacritics(safe(a))
                .equalsIgnoreCase(removeDiacritics(safe(b)));
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
