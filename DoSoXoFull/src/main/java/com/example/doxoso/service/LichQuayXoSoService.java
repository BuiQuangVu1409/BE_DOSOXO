package com.example.doxoso.service;

import com.example.doxoso.model.LichQuayXoSo;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class LichQuayXoSoService {

    // ===== LỊCH QUAY GIỮ NGUYÊN =====
    private static final Map<String, List<String>> LICH_MIEN_BAC = Map.of(
            "Thứ Hai", List.of("HÀ NỘI"),
            "Thứ Ba", List.of("HÀ NỘI"),
            "Thứ Tư", List.of("HÀ NỘI"),
            "Thứ Năm", List.of("HÀ NỘI"),
            "Thứ Sáu", List.of("HÀ NỘI"),
            "Thứ Bảy", List.of("HÀ NỘI"),
            "Chủ Nhật", List.of("HÀ NỘI")
    );

    private static final Map<String, List<String>> LICH_MIEN_TRUNG = Map.of(
            "Thứ Hai", List.of("PHÚ YÊN", "THỪA THIÊN HUẾ"),
            "Thứ Ba", List.of("ĐẮK LẮK", "QUẢNG NAM"),
            "Thứ Tư", List.of("ĐÀ NẴNG", "KHÁNH HÒA"),
            "Thứ Năm", List.of("BÌNH ĐỊNH", "QUẢNG BÌNH", "QUẢNG TRỊ"),
            "Thứ Sáu", List.of("GIA LAI", "NINH THUẬN"),
            "Thứ Bảy", List.of("ĐẮK NÔNG", "QUẢNG NGÃI"),
            "Chủ Nhật", List.of("KON TUM", "KHÁNH HÒA")
    );

    private static final Map<String, List<String>> LICH_MIEN_NAM = Map.of(
            "Thứ Hai", List.of("TP.HỒ CHÍ MINH", "ĐỒNG THÁP", "CÀ MAU"),
            "Thứ Ba", List.of("BẾN TRE", "VŨNG TÀU", "BẠC LIÊU"),
            "Thứ Tư", List.of("CẦN THƠ", "SÓC TRĂNG", "ĐỒNG NAI"),
            "Thứ Năm", List.of("AN GIANG", "TÂY NINH", "BÌNH THUẬN"),
            "Thứ Sáu", List.of("VĨNH LONG", "BÌNH DƯƠNG", "TRÀ VINH"),
            "Thứ Bảy", List.of("TP.HỒ CHÍ MINH", "LONG AN", "BÌNH PHƯỚC", "HẬU GIANG"),
            "Chủ Nhật", List.of("KIÊN GIANG", "TIỀN GIANG", "ĐÀ LẠT")
    );

    // ===== ALIAS phổ biến để khớp linh hoạt (đều đã chuẩn hoá không dấu) =====
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("TP.HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("TP HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("TP HCM", "TP HO CHI MINH"),
            Map.entry("HCM", "TP HO CHI MINH"),
            Map.entry("HO CHI MINH CITY", "TP HO CHI MINH"),
            Map.entry("BA RIA VUNG TAU", "VUNG TAU"),
            Map.entry("BRVT", "VUNG TAU"),
            Map.entry("TP.CAN THO", "CAN THO"),
            Map.entry("TP CAN THO", "CAN THO"),
            Map.entry("DA LAT", "DA LAT")
    );

    // =================== PUBLIC API ===================

    /** Lịch theo ngày (bản gốc có dấu) — dùng cho UI/log. */
    public LichQuayXoSo traCuuTheoNgay(LocalDate ngay) {
        String thu = chuyenDoiThu(ngay);
        Map<String, List<String>> ketQua = new HashMap<>();
        ketQua.put("MIỀN BẮC", LICH_MIEN_BAC.getOrDefault(thu, List.of()));
        ketQua.put("MIỀN TRUNG", LICH_MIEN_TRUNG.getOrDefault(thu, List.of()));
        ketQua.put("MIỀN NAM", LICH_MIEN_NAM.getOrDefault(thu, List.of()));
        // ✅ Model dùng LocalDate -> truyền LocalDate
        return new LichQuayXoSo( ngay, thu, ketQua);
    }

    /**
     * Trả về 3 set (MB/MT/MN) đã CHUẨN HÓA KHÔNG DẤU cho MỘT NGÀY.
     * Dùng để map tên đài -> miền theo ngày.
     */
    public Map<String, Set<String>> mienSetsNormalized(LocalDate ngay) {
        Map<String, Set<String>> out = new HashMap<>();
        out.put("MB", new HashSet<>());
        out.put("MT", new HashSet<>());
        out.put("MN", new HashSet<>());

        LichQuayXoSo lich = traCuuTheoNgay(ngay);
        Map<String, List<String>> m = (lich != null && lich.getKetQua() != null)
                ? lich.getKetQua()
                : Map.of("MIỀN BẮC", List.of(), "MIỀN TRUNG", List.of(), "MIỀN NAM", List.of());

        for (String s : m.get("MIỀN BẮC"))   out.get("MB").add(canonicalProvince(s));
        for (String s : m.get("MIỀN TRUNG")) out.get("MT").add(canonicalProvince(s));
        for (String s : m.get("MIỀN NAM"))   out.get("MN").add(canonicalProvince(s));
        return out;
    }

    /**
     * Trả về 3 set (MB/MT/MN) đã CHUẨN HÓA KHÔNG DẤU cho TOÀN TUẦN (union tất cả các ngày).
     * Dùng làm Fallback để không bỏ sót vé nếu user nhập đài khác ngày quay.
     */
    public Map<String, Set<String>> mienSetsNormalizedAllWeek() {
        Map<String, Set<String>> out = new HashMap<>();
        out.put("MB", normalizeUnion(LICH_MIEN_BAC));
        out.put("MT", normalizeUnion(LICH_MIEN_TRUNG));
        out.put("MN", normalizeUnion(LICH_MIEN_NAM));
        return out;
    }

    /**
     * Xác định mã miền (MB/MT/MN) từ chuỗi đầu vào theo NGÀY:
     * 1) Nhận diện "MB/MT/MN" hoặc "Miền ..."
     * 2) Tra theo lịch NGÀY
     * 3) Fallback tra theo lịch TOÀN TUẦN
     * Trả "" nếu vẫn không xác định được.
     */
    public String regionCodeOf(String provinceOrRegion, LocalDate ngay) {
        if (provinceOrRegion == null || provinceOrRegion.isBlank()) return "";
        String u = normalizeNoAccent(provinceOrRegion);

        // 1) Đã là mã/chuỗi miền
        if (u.equals("MB") || u.startsWith("MB") || u.contains("MIEN BAC"))   return "MB";
        if (u.equals("MT") || u.startsWith("MT") || u.contains("MIEN TRUNG")) return "MT";
        if (u.equals("MN") || u.startsWith("MN") || u.contains("MIEN NAM"))   return "MN";

        // 2) Theo lịch NGÀY
        Map<String, Set<String>> sets = mienSetsNormalized(ngay);
        String token = canonicalProvince(u);
        String code = matchToken(token, sets);
        if (!code.isEmpty()) return code;

        // 3) Fallback TOÀN TUẦN
        Map<String, Set<String>> allWeek = mienSetsNormalizedAllWeek();
        return matchToken(token, allWeek);
    }

    // =================== HELPERS ===================

    private String chuyenDoiThu(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return switch (day) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }

    /** Chuẩn hoá: bỏ dấu, in hoa, gom khoảng trắng & thay alias để so khớp ổn định. */
    private static String normalizeNoAccent(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim()
                .replaceAll("[\\-_.]+", " ")
                .replaceAll("\\s+", " ");
    }

    /** Chuẩn hoá tên đài: áp alias, bỏ tiền tố "TP " để khớp với lịch. */
    private static String canonicalProvince(String raw) {
        String u = normalizeNoAccent(raw);
        String withAlias = ALIASES.getOrDefault(u, u);
        return withAlias.replaceFirst("^TP\\s+", "").trim();
    }

    /** Union & chuẩn hoá một lịch theo tất cả các ngày. */
    private Set<String> normalizeUnion(Map<String, List<String>> lich) {
        Set<String> s = new HashSet<>();
        for (List<String> list : lich.values()) {
            for (String p : list) s.add(canonicalProvince(p));
        }
        return s;
    }

    /** Khớp token với 3 set MB/MT/MN: match chặt -> match lỏng. */
    private String matchToken(String token, Map<String, Set<String>> sets) {
        if (sets.get("MB").contains(token)) return "MB";
        if (sets.get("MT").contains(token)) return "MT";
        if (sets.get("MN").contains(token)) return "MN";
        for (Map.Entry<String, Set<String>> e : sets.entrySet()) {
            for (String canon : e.getValue()) {
                if (token.equals(canon) || token.contains(canon) || canon.contains(token)) {
                    return e.getKey();
                }
            }
        }
        return "";
    }
}
