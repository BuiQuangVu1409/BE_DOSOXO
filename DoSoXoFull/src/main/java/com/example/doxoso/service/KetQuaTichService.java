package com.example.doxoso.service;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.KetQuaTichRepository;
import com.example.doxoso.repository.PlayerRepository;
import com.example.doxoso.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KetQuaTichService {

    private final KetQuaTichRepository ketQuaTichRepo;
    private final BetRepository betRepository;
    private final PlayerRepository playerRepository;

    // nguồn số liệu đã có sẵn
    private final TongTienTrungService tongTienTrungService;                 // tổng TRÚNG theo miền
    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;   // HH + LN + tổng cộng
    private final TongTienAnThuaMienService tongTienAnThuaMienService;       // (không dùng để set tienAnThua ở đây)

    // Dùng lịch quay để map tỉnh/đài -> MB/MT/MN theo NGÀY
    private final LichQuayXoSoService lichQuayXoSoService;

    // ===== Alias tên đài thường gặp (chuẩn hóa để so khớp linh hoạt) =====
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("TP.HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("TP HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("TP HCM", "TP HO CHI MINH"),
            Map.entry("HCM", "TP HO CHI MINH"),
            Map.entry("HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("HO CHI MINH CITY", "TP HO CHI MINH"),
            Map.entry("BA RIA VUNG TAU", "VUNG TAU"),
            Map.entry("BRVT", "VUNG TAU"),
            Map.entry("TAYNINH", "TAY NINH"),
            Map.entry("BINHDUONG", "BINH DUONG"),
            Map.entry("BINHPHUOC", "BINH PHUOC"),
            Map.entry("DA LAT", "DA LAT"), // lịch của bạn dùng "ĐÀ LẠT" (MN - CN)
            Map.entry("CAN THO", "CAN THO"),
            Map.entry("TP.CAN THO", "CAN THO")
    );

    // ===== Helpers chuẩn hoá không dấu =====
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
        // áp alias
        String canon = ALIASES.getOrDefault(u, u);
        // loại tiền tố "TP " nếu có (để khớp với lịch dùng tên tỉnh/đài)
        canon = canon.replaceFirst("^TP\\s+", "").trim();
        return canon;
    }

    /** Tạo 3 set tên đài/tỉnh (đã chuẩn hoá) theo ngày từ LichQuayXoSoService */
    private Map<String, Set<String>> scheduleSets(LocalDate ngay) {
        Map<String, Set<String>> out = new HashMap<>();
        out.put("MB", new HashSet<>());
        out.put("MT", new HashSet<>());
        out.put("MN", new HashSet<>());

        LichQuayXoSo lich = lichQuayXoSoService.traCuuTheoNgay(ngay);
        // giả định LichQuayXoSo có getter getKetQua(): Map<String, List<String>>
        Map<String, List<String>> m = lich.getKetQua();

        for (String key : List.of("MIỀN BẮC", "MIỀN TRUNG", "MIỀN NAM")) {
            List<String> list = m.getOrDefault(key, List.of());
            Set<String> target = switch (key) {
                case "MIỀN BẮC" -> out.get("MB");
                case "MIỀN TRUNG" -> out.get("MT");
                case "MIỀN NAM" -> out.get("MN");
                default -> new HashSet<>();
            };
            for (String province : list) {
                target.add(canonicalProvince(province));
            }
        }
        return out;
    }

    /**
     * Chuẩn hoá miền về MB/MT/MN.
     * 1) Nếu là MB/MT/MN hoặc "MIEN BAC/TRUNG/NAM" -> dùng luôn
     * 2) Ngược lại -> coi như tên đài/tỉnh, tra bảng từ lịch quay theo ngày
     * 3) Nếu vẫn không xác định: trả về token chuẩn hoá (để log)
     */
    private String toCode(String raw, Map<String, Set<String>> scheduleSets) {
        String u = normalizeNoAccent(raw);
        if (u.isEmpty()) return "";

        // mã & tên miền rõ ràng
        if (u.equals("MB") || u.startsWith("MB") || u.contains("MIEN BAC"))   return "MB";
        if (u.equals("MT") || u.startsWith("MT") || u.contains("MIEN TRUNG")) return "MT";
        if (u.equals("MN") || u.startsWith("MN") || u.contains("MIEN NAM"))   return "MN";

        // tra theo lịch
        String token = canonicalProvince(u);
        if (scheduleSets.get("MB").contains(token)) return "MB";
        if (scheduleSets.get("MT").contains(token)) return "MT";
        if (scheduleSets.get("MN").contains(token)) return "MN";

        // thử khớp lỏng (ví dụ "BA RIA VUNG TAU" chứa "VUNG TAU")
        for (Map.Entry<String, Set<String>> e : scheduleSets.entrySet()) {
            for (String canon : e.getValue()) {
                if (token.equals(canon)) return e.getKey();
                if (token.contains(canon) || canon.contains(token)) return e.getKey();
            }
        }

        return u; // để log kiểm tra
    }

    private static String display(String code){
        return switch (code){
            case "MB" -> "MIỀN BẮC";
            case "MT" -> "MIỀN TRUNG";
            case "MN" -> "MIỀN NAM";
            default -> code;
        };
    }

    private static String s(Object x){ return x==null?"":x.toString().trim(); }
    private static boolean isBlank(String x){ return x == null || x.trim().isEmpty(); }

    private static BigDecimal bd(Object x){
        if (x == null) return BigDecimal.ZERO;
        if (x instanceof BigDecimal b) return b;
        if (x instanceof Double d) return BigDecimal.valueOf(d);
        return new BigDecimal(x.toString());
    }

    // Parser CHỈ lấy 1 token tiền hợp lệ, giữ âm/ngoặc, tránh ghép mọi dãy số
    private static BigDecimal parseTienDanh(String s) {
        if (s == null) return BigDecimal.ZERO;

        String n = Normalizer.normalize(s, Normalizer.Form.NFKC).trim();
        if (n.isEmpty()) return BigDecimal.ZERO;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("[-(]?\\d{1,3}([.,\\s]\\d{3})*([.,]\\d+)?[)]?")
                .matcher(n);

        if (!m.find()) return BigDecimal.ZERO;
        String token = m.group();

        boolean negative = token.startsWith("(") || token.startsWith("-");
        token = token.replace("(", "").replace(")", "").replaceAll("\\s+", "");

        if (token.contains(".") && token.contains(",")) {
            token = token.replace(".", "").replace(",", ".");
        } else {
            token = token.replaceAll("\\.(?=\\d{3}(\\D|$))", "");
            if (token.matches(".*\\d,\\d{3}(\\D|$).*")) {
                token = token.replace(",", "");
            } else {
                token = token.replace(",", ".");
            }
        }

        try {
            BigDecimal v = new BigDecimal(token);
            return negative ? v.negate() : v;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Transactional
    public List<KetQuaTich> runAndSaveForPlayer(Long playerId, String playerName, LocalDate ngay) {

        // ---- sets miền theo lịch (chuẩn hoá theo ngày)
        Map<String, Set<String>> sets = scheduleSets(ngay);

        // ===== (1) Tổng TRÚNG -> map theo CODE (MB/MT/MN)
        Map<String, BigDecimal> tienTrungByCode = new HashMap<>();
        TongTienTrungDto trung = tongTienTrungService.tongHopTuDb(playerId, ngay);
        if (trung != null && trung.getCacMien() != null) {
            for (TongTienTrungDto.MienDto m : trung.getCacMien()) {
                String code = toCode(s(m.getMien()), sets); // "MIỀN BẮC" | "MB" | "Nam Định" -> MB/MT/MN
                if (!code.equals("MB") && !code.equals("MT") && !code.equals("MN")) {
                    log.warn("[KQT] Không xác định được miền cho '{}' (player={}, ngay={}) -> bỏ qua để tránh map nhầm",
                            m.getMien(), playerId, ngay);
                    continue;
                }
                tienTrungByCode.merge(code, bd(m.getTongTienMien()), BigDecimal::add);
            }
        }

        // ===== (2) HH + LN + (HH+LN) theo CODE (MB/MT/MN)
        TongHopHoaHongLonNhoDto hhln = tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);

        Map<String, BigDecimal> hhBy = new HashMap<>();
        Map<String, BigDecimal> lnBy = new HashMap<>();
        Map<String, BigDecimal> hhCongLnBy = new HashMap<>();
        if (hhln != null) {
            hhBy.put("MB", bd(hhln.getTongDaNhanHoaHongMB()));
            hhBy.put("MT", bd(hhln.getTongDaNhanHoaHongMT()));
            hhBy.put("MN", bd(hhln.getTongDaNhanHoaHongMN()));

            lnBy.put("MB", bd(hhln.getTienLonNhoMB()));
            lnBy.put("MT", bd(hhln.getTienLonNhoMT()));
            lnBy.put("MN", bd(hhln.getTienLonNhoMN()));

            hhCongLnBy.put("MB", bd(hhln.getTongCongMB()));
            hhCongLnBy.put("MT", bd(hhln.getTongCongMT()));
            hhCongLnBy.put("MN", bd(hhln.getTongCongMN()));
        }

        // ===== (3) KHÔNG dùng service ăn/thua — tự tính theo công thức chuẩn

        // ===== (4) Cộng TIỀN ĐÁNH theo CODE (từ sổ người chơi)
        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
        BigDecimal mb = BigDecimal.ZERO, mt = BigDecimal.ZERO, mn = BigDecimal.ZERO;
        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);
        for (var so : soList) {
            BigDecimal stake = parseTienDanh(so.getSoTien());   // String -> BigDecimal
            String code = toCode(so.getMien(), sets);           // MB/MT/MN hoặc tên đài
            if ("MB".equals(code)) mb = mb.add(stake);
            else if ("MT".equals(code)) mt = mt.add(stake);
            else if ("MN".equals(code)) mn = mn.add(stake);
            else {
                log.warn("[KQT] Bỏ qua bet không xác định miền: mien='{}', soTien='{}' (player={}, ngay={})",
                        so.getMien(), so.getSoTien(), playerId, ngay);
            }
        }
        tienDanhByCode.put("MB", mb);
        tienDanhByCode.put("MT", mt);
        tienDanhByCode.put("MN", mn);

        // ===== (4.1) Resolve playerName nếu không truyền vào
        String resolvedName = playerName;
        if (isBlank(resolvedName) && hhln != null && !isBlank(hhln.getPlayerName())) {
            resolvedName = hhln.getPlayerName();
        }
        if (isBlank(resolvedName)) {
            resolvedName = playerRepository.findById(playerId).map(Player::getName).orElse(null);
        }
        if (isBlank(resolvedName) && !soList.isEmpty() && soList.get(0).getPlayer() != null) {
            resolvedName = soList.get(0).getPlayer().getName();
        }

        // ===== (5) UPsert: nạp snapshot cũ trong ngày → map theo mienCode
        List<KetQuaTich> existedRows = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
        Map<String, KetQuaTich> existedByCode = new HashMap<>();
        for (KetQuaTich r : existedRows) {
            if (r.getMienCode() != null) existedByCode.put(r.getMienCode(), r);
        }

        // ===== (6) Lắp 3 miền MB → MT → MN và upsert
        List<KetQuaTich> rows = new ArrayList<>();
        for (String code : new String[]{"MB", "MT", "MN"}) {
            String display = display(code);

            BigDecimal tienTrung = tienTrungByCode.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienHH    = hhBy.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienLN    = lnBy.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienDanh  = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal danhHH    = hhBy.getOrDefault(code, BigDecimal.ZERO);          // "đã nhận HH"
            BigDecimal danhHH_LN = hhCongLnBy.getOrDefault(code, BigDecimal.ZERO);    // "đã nhận HH + LN"

            // Công thức ăn/thua theo UI: an/thua = TRÚNG + HOA HỒNG − TIỀN ĐÁNH
            BigDecimal tienAT = tienTrung.add(tienHH).subtract(tienDanh);

            if (tienDanh.signum() == 0 && (tienTrung.signum() > 0 || tienHH.signum() > 0)) {
                log.warn("[KQT][ANOMALY] stake=0 nhưng có trung/hh: player={}, ngay={}, code={}, trung={}, hh={}",
                        playerId, ngay, code, tienTrung, tienHH);
            }

            KetQuaTich entity = KetQuaTich.builder()
                    .playerId(playerId)
                    .playerName(resolvedName)
                    .ngay(ngay)
                    .mienCode(code)
                    .mienDisplay(display)
                    .tienTrung(tienTrung)
                    .tienHoaHong(tienHH)
                    .tienLonNho(tienLN)
                    .tienAnThua(tienAT)
                    .tienDanh(tienDanh)
                    .tienDanhDaNhanHoaHong(danhHH)
                    .tienDanhDaNhanHoaHongCongLonNho(danhHH_LN)
                    .build();

            KetQuaTich old = existedByCode.get(code);
            if (old != null) {
                entity.setId(old.getId());
                entity.setVersion(old.getVersion());
                entity.setCreatedAt(old.getCreatedAt());
            }
            rows.add(entity);
        }

        if (log.isInfoEnabled()) {
            log.info("[KQT] player={}, ngay={} | stake(MB/MT/MN)={} / {} / {} | trung={} | hh={} | ln={}",
                    playerId, ngay,
                    tienDanhByCode.get("MB"), tienDanhByCode.get("MT"), tienDanhByCode.get("MN"),
                    tienTrungByCode, hhBy, lnBy);
        }

        return ketQuaTichRepo.saveAll(rows);
    }

    public List<KetQuaTich> findByPlayerAndNgay(Long playerId, LocalDate ngay) {
        return ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
    }
}




//// com.example.doxoso.service.KetQuaTichService.java
//package com.example.doxoso.service;
//
//import com.example.doxoso.model.*;
//import com.example.doxoso.repository.KetQuaTichRepository;
//import com.example.doxoso.repository.PlayerRepository;
//import com.example.doxoso.repository.BetRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.text.Normalizer;
//import java.time.LocalDate;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class KetQuaTichService {
//
//    private final KetQuaTichRepository ketQuaTichRepo;
//    private final BetRepository betRepository;
//    private final PlayerRepository playerRepository;
//
//    // nguồn số liệu đã có sẵn
//    private final TongTienTrungService tongTienTrungService;                 // tổng TRÚNG theo miền
//    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;   // HH + LN + tổng cộng
//    // Giữ lại nếu nơi khác còn dùng, còn tại đây ta tự tính An/Thua:
//    private final TongTienAnThuaMienService tongTienAnThuaMienService;       // (không còn dùng để set tienAnThua)
//
//    @Transactional
//    public List<KetQuaTich> runAndSaveForPlayer(Long playerId, String playerName, LocalDate ngay) {
//        // ===== (1) Tổng TRÚNG -> map theo CODE (MB/MT/MN), tránh lệch key
//        Map<String, BigDecimal> tienTrungByCode = new HashMap<>();
//        TongTienTrungDto trung = tongTienTrungService.tongHopTuDb(playerId, ngay);
//        if (trung != null && trung.getCacMien() != null) {
//            for (TongTienTrungDto.MienDto m : trung.getCacMien()) {
//                String code = toCode(s(m.getMien())); // "MIỀN BẮC" | "MB" -> "MB"
//                tienTrungByCode.merge(code, bd(m.getTongTienMien()), BigDecimal::add);
//            }
//        }
//
//        // ===== (2) HH + LN + (HH+LN) theo CODE (MB/MT/MN)
//        TongHopHoaHongLonNhoDto hhln = tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);
//
//        Map<String, BigDecimal> hhBy = new HashMap<>();
//        Map<String, BigDecimal> lnBy = new HashMap<>();
//        Map<String, BigDecimal> hhCongLnBy = new HashMap<>();
//        if (hhln != null) {
//            hhBy.put("MB", bd(hhln.getTongDaNhanHoaHongMB()));
//            hhBy.put("MT", bd(hhln.getTongDaNhanHoaHongMT()));
//            hhBy.put("MN", bd(hhln.getTongDaNhanHoaHongMN()));
//
//            lnBy.put("MB", bd(hhln.getTienLonNhoMB()));
//            lnBy.put("MT", bd(hhln.getTienLonNhoMT()));
//            lnBy.put("MN", bd(hhln.getTienLonNhoMN()));
//
//            hhCongLnBy.put("MB", bd(hhln.getTongCongMB()));
//            hhCongLnBy.put("MT", bd(hhln.getTongCongMT()));
//            hhCongLnBy.put("MN", bd(hhln.getTongCongMN()));
//        }
//
//        // ===== (3) KHÔNG gọi service ăn/thua nữa — tự tính theo công thức chuẩn
//
//        // ===== (4) Cộng TIỀN ĐÁNH theo CODE (từ sổ người chơi)
//        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
//        BigDecimal mb = BigDecimal.ZERO, mt = BigDecimal.ZERO, mn = BigDecimal.ZERO;
//        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);
//        for (var so : soList) {
//            BigDecimal stake = parseTienDanh(so.getSoTien()); // String -> BigDecimal
//            String code = toCode(so.getMien());                 // MB/MT/MN
//            if ("MB".equals(code)) mb = mb.add(stake);
//            else if ("MT".equals(code)) mt = mt.add(stake);
//            else if ("MN".equals(code)) mn = mn.add(stake);
//        }
//        tienDanhByCode.put("MB", mb);
//        tienDanhByCode.put("MT", mt);
//        tienDanhByCode.put("MN", mn);
//
//        // ===== (4.1) Resolve playerName nếu không truyền vào
//        String resolvedName = playerName;
//        if (isBlank(resolvedName) && hhln != null && !isBlank(hhln.getPlayerName())) {
//            resolvedName = hhln.getPlayerName();
//        }
//        if (isBlank(resolvedName)) {
//            resolvedName = playerRepository.findById(playerId).map(Player::getName).orElse(null);
//        }
//        if (isBlank(resolvedName) && !soList.isEmpty() && soList.get(0).getPlayer() != null) {
//            resolvedName = soList.get(0).getPlayer().getName();
//        }
//
//        // ===== (5) UPsert: nạp sẵn snapshot cũ trong ngày → map theo mienCode
//        List<KetQuaTich> existedRows = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
//        Map<String, KetQuaTich> existedByCode = new HashMap<>();
//        for (KetQuaTich r : existedRows) {
//            if (r.getMienCode() != null) {
//                existedByCode.put(r.getMienCode(), r);
//            }
//        }
//
//        // ===== (6) Lắp 3 miền MB → MT → MN và upsert
//        List<KetQuaTich> rows = new ArrayList<>();
//        for (String code : new String[]{"MB", "MT", "MN"}) {
//            String display = display(code);
//
//            BigDecimal tienTrung  = tienTrungByCode.getOrDefault(code, BigDecimal.ZERO);
//            BigDecimal tienHH     = hhBy.getOrDefault(code, BigDecimal.ZERO);
//            BigDecimal tienLN     = lnBy.getOrDefault(code, BigDecimal.ZERO);
//            BigDecimal tienDanh   = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);
//            BigDecimal danhHH     = hhBy.getOrDefault(code, BigDecimal.ZERO);          // đang hiểu là "đã nhận HH"
//            BigDecimal danhHH_LN  = hhCongLnBy.getOrDefault(code, BigDecimal.ZERO);    // "đã nhận HH + LN"
//
//            // TỰ TÍNH ĂN/THUA ở đây
//            BigDecimal tienAT = tienTrung.subtract(tienHH.add(tienLN));
//
//            KetQuaTich entity = KetQuaTich.builder()
//                    .playerId(playerId)
//                    .playerName(resolvedName)
//                    .ngay(ngay)
//                    .mienCode(code)
//                    .mienDisplay(display)
//                    .tienTrung(tienTrung)
//                    .tienHoaHong(tienHH)
//                    .tienLonNho(tienLN)
//                    .tienAnThua(tienAT)
//                    .tienDanh(tienDanh)
//                    .tienDanhDaNhanHoaHong(danhHH)
//                    .tienDanhDaNhanHoaHongCongLonNho(danhHH_LN)
//                    .build();
//
//            KetQuaTich old = existedByCode.get(code);
//            if (old != null) {
//                entity.setId(old.getId());             // UPDATE thay vì INSERT
//                entity.setVersion(old.getVersion());   // nếu có @Version
//                entity.setCreatedAt(old.getCreatedAt());// nếu có field
//            }
//            rows.add(entity);
//        }
//
//        return ketQuaTichRepo.saveAll(rows);
//    }
//
//    public List<KetQuaTich> findByPlayerAndNgay(Long playerId, LocalDate ngay) {
//        return ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
//    }
//
//    // ------- helpers -------
//    private static String s(Object x){ return x==null?"":x.toString().trim(); }
//    private static boolean isBlank(String x){ return x == null || x.trim().isEmpty(); }
//
//    private static BigDecimal bd(Object x){
//        if (x == null) return BigDecimal.ZERO;
//        if (x instanceof BigDecimal b) return b;
//        if (x instanceof Double d) return BigDecimal.valueOf(d);
//        return new BigDecimal(x.toString());
//    }
//
//    private static String toCode(String raw){
//        if (raw == null) return "";
//        String u = raw.trim().toUpperCase();
//        if (u.startsWith("MB") || u.contains("BẮC") || u.contains("BAC")) return "MB";
//        if (u.startsWith("MT") || u.contains("TRUNG"))                    return "MT";
//        if (u.startsWith("MN") || u.contains("NAM"))                      return "MN";
//        return u;
//    }
//
//    private static String display(String code){
//        return switch (code){
//            case "MB" -> "MIỀN BẮC";
//            case "MT" -> "MIỀN TRUNG";
//            case "MN" -> "MIỀN NAM";
//            default -> code;
//        };
//    }
//
//    // Parser CHỈ lấy 1 token tiền hợp lệ, giữ âm/ngoặc, tránh ghép mọi dãy số
//    private static BigDecimal parseTienDanh(String s) {
//        if (s == null) return BigDecimal.ZERO;
//
//        String n = Normalizer.normalize(s, Normalizer.Form.NFKC).trim();
//        if (n.isEmpty()) return BigDecimal.ZERO;
//
//        java.util.regex.Matcher m = java.util.regex.Pattern
//                .compile("[-(]?\\d{1,3}([.,\\s]\\d{3})*([.,]\\d+)?[)]?")
//                .matcher(n);
//
//        if (!m.find()) return BigDecimal.ZERO;
//        String token = m.group();
//
//        boolean negative = token.startsWith("(") || token.startsWith("-");
//        token = token.replace("(", "").replace(")", "").replaceAll("\\s+", "");
//
//        if (token.contains(".") && token.contains(",")) {
//            // ví dụ: 1.234.567,89  -> 1234567.89
//            token = token.replace(".", "").replace(",", ".");
//        } else {
//            // bỏ dấu nhóm ngàn nếu là . và theo sau là 3 chữ số
//            token = token.replaceAll("\\.(?=\\d{3}(\\D|$))", "");
//            // nếu mẫu giống nhóm ngàn với dấu phẩy thì bỏ phẩy, ngược lại coi phẩy là thập phân
//            if (token.matches(".*\\d,\\d{3}(\\D|$).*")) {
//                token = token.replace(",", "");
//            } else {
//                token = token.replace(",", ".");
//            }
//        }
//
//        try {
//            BigDecimal v = new BigDecimal(token);
//            return negative ? v.negate() : v;
//        } catch (NumberFormatException e) {
//            return BigDecimal.ZERO;
//        }
//    }
//}
