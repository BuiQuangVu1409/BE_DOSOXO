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
public class LonService {

    @Autowired private KetQuaMienBacRepository bacRepo;
    @Autowired private KetQuaMienTrungRepository trungRepo;
    @Autowired private KetQuaMienNamRepository namRepo;
    @Autowired private TinhTienService tinhTienService;

    // Mã nội bộ cho 3 loại LỚN
    private static final String LON_DAU      = "LON_DAU";
    private static final String LON_DUOI     = "LON_DUOI";
    private static final String LON_DAUDUOI  = "LON_DAUDUOI";

    /**
     * Xử lý LỚN theo đúng loại:
     * - cachDanh: "LỚN ĐẦU", "LỚN ĐUÔI", "LỚN ĐẦU ĐUÔI" (hoặc biến thể không dấu)
     */
    public DoiChieuKetQuaDto xuLyLon(Long playerId,
                                     String cachDanh,  // 👈 MỚI: cách đánh người chơi nhập
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

        // 0) Chuẩn hóa loại LỚN (ĐẦU / ĐUÔI / ĐẦU ĐUÔI)
        String loaiLon = chuanHoaCachDanhLon(cachDanh);

        if (loaiLon == null) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setCachDanh(safe(cachDanh));
            dto.setSaiLyDo(List.of(
                    "Cách đánh LỚN không hợp lệ. Phải là: LỚN ĐẦU, LỚN ĐUÔI hoặc LỚN ĐẦU ĐUÔI"
            ));
            return dto;
        }

        // 1) Validate tiền đánh
        if (tienDanh == null || tienDanh.isBlank()) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setCachDanh(hienThiCachDanh(loaiLon));
            dto.setSaiLyDo(List.of("Thiếu tiền đánh"));
            return dto;
        }

        // 2) Điều hướng theo loại
        dto.setCachDanh(hienThiCachDanh(loaiLon));

        return switch (loaiLon) {
            case LON_DAU     -> xuLyLonDau(playerId, dto, mien, tenDai, ngay, tienDanh);
            case LON_DUOI    -> xuLyLonDuoi(playerId, dto, mien, tenDai, ngay, tienDanh);
            case LON_DAUDUOI -> xuLyLonDauDuoi(playerId, dto, mien, tenDai, ngay, tienDanh);
            default -> {
                dto.setTrung(false);
                dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of("Cách đánh LỚN không hỗ trợ: " + safe(cachDanh)));
                yield dto;
            }
        };
    }

    /* ======================= LỚN ĐẦU ======================= */

    private DoiChieuKetQuaDto xuLyLonDau(Long playerId,
                                         DoiChieuKetQuaDto dto,
                                         String mien,
                                         String tenDai,
                                         LocalDate ngay,
                                         String tienDanh) {
        String codeMien = toMienCode(mien);

        // LỚN ĐẦU chỉ áp dụng cho MT, MN
        if (!"MT".equals(codeMien) && !"MN".equals(codeMien)) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setCachTrung("LỚN ĐẦU không áp dụng cho " + safe(mien));
            dto.setSaiLyDo(List.of("LỚN ĐẦU chỉ áp dụng cho MIỀN TRUNG và MIỀN NAM"));
            return dto;
        }

        String ketQuaGiai8 = timGiaiTamTheoMien(mien, tenDai, ngay);
        if (ketQuaGiai8 == null) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setCachTrung("Không tìm thấy Giải 8 cho đài " + safe(tenDai));
            dto.setSaiLyDo(List.of("Không tìm thấy Giải 8 để dò LỚN ĐẦU"));
            return dto;
        }

        Integer duoiG8 = duoi2So(ketQuaGiai8);
        if (duoiG8 == null) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không tách được 2 số cuối từ Giải 8: " + ketQuaGiai8));
            return dto;
        }

        boolean trungLonDau = isLon(duoiG8); // 50–99

        dto.setTrung(trungLonDau);
        if (trungLonDau) {
            dto.setCachTrung("LỚN ĐẦU – Giải 8: " + duoiG8);
            dto.setGiaiTrung("LỚN ĐẦU");
            dto.setTienTrung(tinhTienService.tinhTienLon(playerId, "LON_DAU", tienDanh, mien));
        } else {
            dto.setCachTrung("Trật LỚN ĐẦU – Giải 8: " + duoiG8);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Đầu Giải 8 không nằm trong khoảng 50–99"));
        }

        return dto;
    }

    /* ======================= LỚN ĐUÔI ====================== */

    private DoiChieuKetQuaDto xuLyLonDuoi(Long playerId,
                                          DoiChieuKetQuaDto dto,
                                          String mien,
                                          String tenDai,
                                          LocalDate ngay,
                                          String tienDanh) {

        String ketQuaDb = timKetQuaTheoMien(mien, tenDai, ngay);

        // Validate Giải ĐB theo MB/MT/MN
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

        boolean trungLonDuoi = isLon(duoiDb); // 50–99

        dto.setTrung(trungLonDuoi);
        if (trungLonDuoi) {
            dto.setCachTrung("LỚN ĐUÔI – Giải ĐB: " + duoiDb);
            dto.setGiaiTrung("LỚN ĐUÔI");
            dto.setTienTrung(tinhTienService.tinhTienLon(playerId, "LON_DUOI", tienDanh, mien));
        } else {
            dto.setCachTrung("Trật LỚN ĐUÔI – Giải ĐB: " + duoiDb);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Đuôi Giải ĐB không nằm trong khoảng 50–99"));
        }

        return dto;
    }

    /* ==================== LỚN ĐẦU ĐUÔI ===================== */

    private DoiChieuKetQuaDto xuLyLonDauDuoi(Long playerId,
                                             DoiChieuKetQuaDto dto,
                                             String mien,
                                             String tenDai,
                                             LocalDate ngay,
                                             String tienDanh) {

        String codeMien = toMienCode(mien);

        // --- Phần ĐUÔI: Giải ĐB (MB/MT/MN) ---
        String ketQuaDb = timKetQuaTheoMien(mien, tenDai, ngay);
        Integer duoiDb = null;
        boolean trungLonDuoi = false;

        if (ketQuaDb != null && isValidGiaiDbForMien(mien, ketQuaDb)) {
            duoiDb = duoi2So(ketQuaDb);
            if (duoiDb != null) {
                trungLonDuoi = isLon(duoiDb);
            }
        }

        // --- Phần ĐẦU: Giải 8 (chỉ MT, MN) ---
        Integer duoiG8 = null;
        boolean trungLonDau = false;
        if ("MT".equals(codeMien) || "MN".equals(codeMien)) {
            String ketQuaG8 = timGiaiTamTheoMien(mien, tenDai, ngay);
            if (ketQuaG8 != null) {
                duoiG8 = duoi2So(ketQuaG8);
                if (duoiG8 != null) {
                    trungLonDau = isLon(duoiG8);
                }
            }
        }

        boolean trungLon = trungLonDau || trungLonDuoi;
        dto.setTrung(trungLon);

        if (trungLon) {
            StringBuilder cachTrung = new StringBuilder();

            if (trungLonDau) {
                cachTrung.append("LỚN ĐẦU – Giải 8: ")
                        .append(duoiG8 != null ? duoiG8 : "??");
            }
            if (trungLonDuoi) {
                if (cachTrung.length() > 0) cachTrung.append(" | ");
                cachTrung.append("LỚN ĐUÔI – Giải ĐB: ")
                        .append(duoiDb != null ? duoiDb : "??");
            }

            dto.setCachTrung(cachTrung.toString());

            if (trungLonDau && trungLonDuoi) {
                dto.setGiaiTrung("LỚN ĐẦU & LỚN ĐUÔI");
            } else if (trungLonDau) {
                dto.setGiaiTrung("LỚN ĐẦU");
            } else {
                dto.setGiaiTrung("LỚN ĐUÔI");
            }

            // Tiền: mặc định mỗi vé LỚN ĐẦU ĐUÔI ăn 1 lần,
            // nếu bạn muốn trúng cả ĐẦU + ĐUÔI ăn gấp đôi thì nhân thêm hệ số.
            double base = tinhTienService.tinhTienLon(playerId, "LON_DAUDUOI", tienDanh, mien);
            int heSo = 0;
            if (trungLonDau) heSo++;
            if (trungLonDuoi) heSo++;
            dto.setTienTrung(base * heSo); // trúng 1 phần = 1 lần, trúng cả 2 = 2 lần

        } else {
            StringBuilder sb = new StringBuilder("Trật LỚN ĐẦU ĐUÔI – ");
            sb.append("Giải ĐB: ").append(duoiDb != null ? duoiDb : "không có");
            if ("MT".equals(codeMien) || "MN".equals(codeMien)) {
                sb.append(" | Giải 8: ").append(duoiG8 != null ? duoiG8 : "không có");
            } else {
                sb.append(" (LỚN ĐẦU không áp dụng cho miền Bắc)");
            }

            dto.setCachTrung(sb.toString());
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Đầu/đuôi không nằm trong khoảng 50–99"));
        }

        return dto;
    }

    /* ===================== Chuẩn hóa mã miền ===================== */

    private String toMienCode(String mien) {
        String u = removeDiacritics(safe(mien)).toUpperCase();
        if (u.equals("MB") || u.contains("BAC"))   return "MB";
        if (u.equals("MT") || u.contains("TRUNG")) return "MT";
        if (u.equals("MN") || u.contains("NAM"))   return "MN";
        return "??";
    }

    /**
     * Lấy số trúng của Giải ĐẶC BIỆT cho đúng đài/ngày/miền (dùng cho LỚN ĐUÔI).
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
     * Lấy số trúng của Giải 8 cho đúng đài/ngày/miền (dùng cho LỚN ĐẦU).
     * - Chỉ áp dụng cho MT, MN.
     * - Miền Bắc không có LỚN ĐẦU → luôn trả null.
     */
    private String timGiaiTamTheoMien(String mien, String tenDai, LocalDate ngay) {
        String code = toMienCode(mien);
        String tenDaiNorm = safe(tenDai);

        if ("MT".equals(code)) {
            return trungRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiTam(k.getGiai()))
                    .map(k -> k.getSoTrung())
                    .findFirst()
                    .orElse(null);
        } else if ("MN".equals(code)) {
            return namRepo.findAllByNgay(ngay).stream()
                    .filter(k -> equalsNoAccent(k.getTenDai(), tenDaiNorm) && laGiaiTam(k.getGiai()))
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
            case "MB"      -> digits.length() == 5;
            case "MT","MN" -> digits.length() == 6;
            default        -> false;
        };
    }

    /** Nhận diện "Đặc Biệt" theo nhiều dạng, bỏ dấu/ký tự đặc biệt và so sánh */
    private boolean laGiaiDacBiet(String giai) {
        String norm = removeDiacritics(safe(giai)).toUpperCase().replaceAll("[^A-Z0-9]", "");
        return norm.equals("DACBIET")
                || norm.equals("DB")
                || norm.equals("GDB")
                || norm.equals("GIAIDACBIET");
    }

    /** Nhận diện "Giải 8" theo nhiều dạng: "Giải tám", "Giải 8", "G8", "Giai8"... */
    private boolean laGiaiTam(String giai) {
        String norm = removeDiacritics(safe(giai)).toUpperCase().replaceAll("[^A-Z0-9]", "");
        return norm.equals("GIAITAM")
                || norm.equals("TAM")
                || norm.equals("G8")
                || norm.equals("GIAI8");
    }

    /** Lấy 2 số cuối an toàn */
    private Integer duoi2So(String soGiai) {
        if (soGiai == null) return null;
        String s = soGiai.replaceAll("\\D", "");
        if (s.length() < 2) return null;
        return Integer.parseInt(s.substring(s.length() - 2));
    }

    /** Điều kiện LỚN: 50–99 */
    private boolean isLon(int duoi) {
        return duoi >= 50 && duoi <= 99;
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

    /* ================== Chuẩn hóa CÁCH ĐÁNH ================== */

    private String chuanHoaCachDanhLon(String raw) {
        if (raw == null) return null;
        String cd = removeDiacritics(raw)
                .toUpperCase()
                .replaceAll("[\\s_]+", ""); // bỏ khoảng trắng, underscore

        boolean hasLon  = cd.contains("LON");
        boolean hasDau  = cd.contains("DAU");
        boolean hasDuoi = cd.contains("DUOI");

        if (hasLon && hasDau && hasDuoi) return LON_DAUDUOI;
        if (hasLon && hasDau)            return LON_DAU;
        if (hasLon && hasDuoi)           return LON_DUOI;

        return null;
    }

    private String hienThiCachDanh(String loaiLon) {
        return switch (loaiLon) {
            case LON_DAU     -> "LỚN ĐẦU";
            case LON_DUOI    -> "LỚN ĐUÔI";
            case LON_DAUDUOI -> "LỚN ĐẦU ĐUÔI";
            default          -> "LỚN";
        };
    }
}
