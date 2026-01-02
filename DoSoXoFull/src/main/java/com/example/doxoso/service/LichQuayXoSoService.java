package com.example.doxoso.service;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LichQuayXoSoService {

    // ====== REPOSITORY ĐỂ LẤY LỊCH & BẢNG GIẢI TỪ DB ======
    @Autowired
    private KetQuaMienBacRepository bacRepo;
    @Autowired
    private KetQuaMienTrungRepository trungRepo;
    @Autowired
    private KetQuaMienNamRepository namRepo;

    // ===== LỊCH QUAY GIỮ NGUYÊN (STATIC) =====
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

    // ===== THỨ TỰ GIẢI ĐỂ VẼ BẢNG =====
    private static final LinkedHashMap<String, Integer> PRIZE_ORDER = new LinkedHashMap<>();
    static {
        PRIZE_ORDER.put("ĐẶC BIỆT", 1);
        PRIZE_ORDER.put("DB", 1);
        PRIZE_ORDER.put("ĐB", 1);
        PRIZE_ORDER.put("G1", 2);
        PRIZE_ORDER.put("G2", 3);
        PRIZE_ORDER.put("G3", 4);
        PRIZE_ORDER.put("G4", 5);
        PRIZE_ORDER.put("G5", 6);
        PRIZE_ORDER.put("G6", 7);
        PRIZE_ORDER.put("G7", 8);
    }

    // =================== PUBLIC API CŨ (LỊCH STATIC) ===================

    /** Lịch theo ngày (bản gốc có dấu) — dùng cho UI/log. */
    public LichQuayXoSo traCuuTheoNgay(LocalDate ngay) {
        String thu = chuyenDoiThu(ngay);
        Map<String, List<String>> ketQua = new HashMap<>();
        ketQua.put("MIỀN BẮC", LICH_MIEN_BAC.getOrDefault(thu, List.of()));
        ketQua.put("MIỀN TRUNG", LICH_MIEN_TRUNG.getOrDefault(thu, List.of()));
        ketQua.put("MIỀN NAM", LICH_MIEN_NAM.getOrDefault(thu, List.of()));
        return new LichQuayXoSo(ngay, thu, ketQua);
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

    /** Toàn tuần, dùng fallback cho dò số. */
    public Map<String, Set<String>> mienSetsNormalizedAllWeek() {
        Map<String, Set<String>> out = new HashMap<>();
        out.put("MB", normalizeUnion(LICH_MIEN_BAC));
        out.put("MT", normalizeUnion(LICH_MIEN_TRUNG));
        out.put("MN", normalizeUnion(LICH_MIEN_NAM));
        return out;
    }

    /** Xác định MB/MT/MN từ tên đài/nguyên miền. */
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

    // =================== API MỚI 1: LỊCH QUAY TỪ DB ===================

    public List<LichQuayXoSo> traCuuLichQuayTheoKetQua(LocalDate from, LocalDate to) {
        List<KetQuaMienBac> bac   = bacRepo.findByNgayBetween(from, to);
        List<KetQuaMienTrung> trung = trungRepo.findByNgayBetween(from, to);
        List<KetQuaMienNam> nam   = namRepo.findByNgayBetween(from, to);

        Map<LocalDate, LichQuayXoSo> mapByDate = new TreeMap<>();

        // ===== GOM MIỀN BẮC =====
        for (KetQuaMienBac kq : bac) {
            LocalDate ngay = kq.getNgay();
            LichQuayXoSo lich = mapByDate.computeIfAbsent(
                    ngay,
                    d -> new LichQuayXoSo(d, chuyenDoiThu(d), new HashMap<>())
            );
            Map<String, List<String>> ketQua = lich.getKetQua();
            List<String> dsDai = ketQua.computeIfAbsent("MIỀN BẮC", k -> new ArrayList<>());
            String tenDai = kq.getTenDai();
            if (tenDai != null && !dsDai.contains(tenDai)) {
                dsDai.add(tenDai);
            }
        }

        // ===== GOM MIỀN TRUNG =====
        for (KetQuaMienTrung kq : trung) {
            LocalDate ngay = kq.getNgay();
            LichQuayXoSo lich = mapByDate.computeIfAbsent(
                    ngay,
                    d -> new LichQuayXoSo(d, chuyenDoiThu(d), new HashMap<>())
            );
            Map<String, List<String>> ketQua = lich.getKetQua();
            List<String> dsDai = ketQua.computeIfAbsent("MIỀN TRUNG", k -> new ArrayList<>());
            String tenDai = kq.getTenDai();
            if (tenDai != null && !dsDai.contains(tenDai)) {
                dsDai.add(tenDai);
            }
        }

        // ===== GOM MIỀN NAM =====
        for (KetQuaMienNam kq : nam) {
            LocalDate ngay = kq.getNgay();
            LichQuayXoSo lich = mapByDate.computeIfAbsent(
                    ngay,
                    d -> new LichQuayXoSo(d, chuyenDoiThu(d), new HashMap<>())
            );
            Map<String, List<String>> ketQua = lich.getKetQua();
            List<String> dsDai = ketQua.computeIfAbsent("MIỀN NAM", k -> new ArrayList<>());
            String tenDai = kq.getTenDai();
            if (tenDai != null && !dsDai.contains(tenDai)) {
                dsDai.add(tenDai);
            }
        }

        // sort tên đài trong từng miền
        for (LichQuayXoSo lich : mapByDate.values()) {
            lich.getKetQua().replaceAll((mien, ds) ->
                    ds.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList())
            );
        }

        return new ArrayList<>(mapByDate.values());
    }

    // =================== API MỚI 2: BẢNG GIẢI NHƯ ẢNH ===================

    /** Lấy bảng kết quả chi tiết (ĐB, G1..G7) cho 1 ngày + 1 miền. */
    public LichDoMienDto getBangKetQua(LocalDate ngay, String mienCode) {
        String mienDisplay = toMienDisplay(mienCode);

        switch (mienCode.toUpperCase()) {
            case "MB":
                return buildMienBac(ngay, mienDisplay);
            case "MT":
                return buildMienTrung(ngay, mienDisplay);
            case "MN":
                return buildMienNam(ngay, mienDisplay);
            default:
                throw new IllegalArgumentException("Miền không hợp lệ: " + mienCode);
        }
    }

    // --- miền Bắc: 1 đài/ngày ---
    private LichDoMienDto buildMienBac(LocalDate ngay, String mienDisplay) {
        List<KetQuaMienBac> list = bacRepo.findAllByNgay(ngay);

        Map<String, List<String>> bangKetQua = groupByPrize(
                list,
                KetQuaMienBac::getGiai,
                KetQuaMienBac::getSoTrung
        );

        String thu = chuyenDoiThu(ngay);
        String tenDai = list.isEmpty() ? "HÀ NỘI" : list.get(0).getTenDai();

        BangKetQuaDaiDto dai = BangKetQuaDaiDto.builder()
                .mien(mienDisplay)
                .tenDai(tenDai)
                .ngay(ngay)
                .thu(thu)
                .bangKetQua(bangKetQua)
                .build();

        return LichDoMienDto.builder()
                .mien(mienDisplay)
                .ngay(ngay)
                .thu(thu)
                .danhSachDai(Collections.singletonList(dai))
                .build();
    }

    // --- miền Trung: nhiều đài/ngày ---
    private LichDoMienDto buildMienTrung(LocalDate ngay, String mienDisplay) {
        List<KetQuaMienTrung> list = trungRepo.findAllByNgay(ngay);
        Map<String, List<KetQuaMienTrung>> byDai =
                list.stream().collect(Collectors.groupingBy(KetQuaMienTrung::getTenDai));

        String thu = chuyenDoiThu(ngay);
        List<BangKetQuaDaiDto> ds = new ArrayList<>();

        for (Map.Entry<String, List<KetQuaMienTrung>> e : byDai.entrySet()) {
            String tenDai = e.getKey();
            Map<String, List<String>> bangKetQua = groupByPrize(
                    e.getValue(),
                    KetQuaMienTrung::getGiai,
                    KetQuaMienTrung::getSoTrung
            );

            ds.add(BangKetQuaDaiDto.builder()
                    .mien(mienDisplay)
                    .tenDai(tenDai)
                    .ngay(ngay)
                    .thu(thu)
                    .bangKetQua(bangKetQua)
                    .build());
        }

        ds.sort(Comparator.comparing(BangKetQuaDaiDto::getTenDai));

        return LichDoMienDto.builder()
                .mien(mienDisplay)
                .ngay(ngay)
                .thu(thu)
                .danhSachDai(ds)
                .build();
    }

    // --- miền Nam: nhiều đài/ngày ---
    private LichDoMienDto buildMienNam(LocalDate ngay, String mienDisplay) {
        List<KetQuaMienNam> list = namRepo.findAllByNgay(ngay);
        Map<String, List<KetQuaMienNam>> byDai =
                list.stream().collect(Collectors.groupingBy(KetQuaMienNam::getTenDai));

        String thu = chuyenDoiThu(ngay);
        List<BangKetQuaDaiDto> ds = new ArrayList<>();

        for (Map.Entry<String, List<KetQuaMienNam>> e : byDai.entrySet()) {
            String tenDai = e.getKey();
            Map<String, List<String>> bangKetQua = groupByPrize(
                    e.getValue(),
                    KetQuaMienNam::getGiai,
                    KetQuaMienNam::getSoTrung
            );

            ds.add(BangKetQuaDaiDto.builder()
                    .mien(mienDisplay)
                    .tenDai(tenDai)
                    .ngay(ngay)
                    .thu(thu)
                    .bangKetQua(bangKetQua)
                    .build());
        }

        ds.sort(Comparator.comparing(BangKetQuaDaiDto::getTenDai));

        return LichDoMienDto.builder()
                .mien(mienDisplay)
                .ngay(ngay)
                .thu(thu)
                .danhSachDai(ds)
                .build();
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

    private static String normalizeNoAccent(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim()
                .replaceAll("[\\-_.]+", " ")
                .replaceAll("\\s+", " ");
    }

    private static String canonicalProvince(String raw) {
        String u = normalizeNoAccent(raw);
        String withAlias = ALIASES.getOrDefault(u, u);
        return withAlias.replaceFirst("^TP\\s+", "").trim();
    }

    private Set<String> normalizeUnion(Map<String, List<String>> lich) {
        Set<String> s = new HashSet<>();
        for (List<String> list : lich.values()) {
            for (String p : list) s.add(canonicalProvince(p));
        }
        return s;
    }

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

    private String toMienDisplay(String mienCode) {
        switch (mienCode.toUpperCase()) {
            case "MB": return "MIỀN BẮC";
            case "MT": return "MIỀN TRUNG";
            case "MN": return "MIỀN NAM";
            default:    return mienCode;
        }
    }

    private String normalizePrize(String raw) {
        if (raw == null) return "";
        String g = raw.trim().toUpperCase();
        if (g.contains("ĐẶC BIỆT") || g.equals("ĐB") || g.equals("DB")) {
            return "ĐẶC BIỆT";
        }
        return g;
    }

    private <T> Map<String, List<String>> groupByPrize(
            List<T> list,
            Function<T, String> prizeGetter,
            Function<T, String> numGetter
    ) {
        Map<String, List<String>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        item -> normalizePrize(prizeGetter.apply(item)),
                        Collectors.mapping(numGetter, Collectors.toList())
                ));

        LinkedHashMap<String, List<String>> ordered = new LinkedHashMap<>();
        grouped.entrySet().stream()
                .sorted(Comparator.comparingInt(
                        e -> PRIZE_ORDER.getOrDefault(e.getKey(), 999)
                ))
                .forEachOrdered(e -> ordered.put(e.getKey(), e.getValue()));

        return ordered;
    }
}
