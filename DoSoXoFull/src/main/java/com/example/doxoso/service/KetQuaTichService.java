package com.example.doxoso.service;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.KetQuaTichRepository;
import com.example.doxoso.repository.BetRepository;
import com.example.doxoso.repository.PlayerRepository;
import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final KetQuaNguoiChoiRepository ketQuaNguoiChoiRepo;

    private final TongTienTrungService tongTienTrungService;
    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;
    private final TongTienAnThuaMienService tongTienAnThuaMienService;
    private final LichQuayXoSoService lichQuayXoSoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== normalize / alias =====
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
            Map.entry("DA LAT", "DA LAT"),
            Map.entry("CAN THO", "CAN THO"),
            Map.entry("TP.CAN THO", "CAN THO")
    );

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
        String canon = ALIASES.getOrDefault(u, u);
        canon = canon.replaceFirst("^TP\\s+", "").trim();
        return canon;
    }

    private Map<String, Set<String>> scheduleSets(LocalDate ngay) {
        Map<String, Set<String>> out = new HashMap<>();
        out.put("MB", new HashSet<>());
        out.put("MT", new HashSet<>());
        out.put("MN", new HashSet<>());

        LichQuayXoSo lich = lichQuayXoSoService.traCuuTheoNgay(ngay);
        Map<String, List<String>> m = lich.getKetQua();

        for (String key : List.of("MIỀN BẮC", "MIỀN TRUNG", "MIỀN NAM")) {
            List<String> list = m.getOrDefault(key, List.of());
            Set<String> target = switch (key) {
                case "MIỀN BẮC" -> out.get("MB");
                case "MIỀN TRUNG" -> out.get("MT");
                case "MIỀN NAM" -> out.get("MN");
                default -> new HashSet<>();
            };
            for (String province : list) target.add(canonicalProvince(province));
        }
        return out;
    }

    private String toCode(String raw, Map<String, Set<String>> sets) {
        String u = normalizeNoAccent(raw);
        if (u.isEmpty()) return "";
        if (u.startsWith("MB") || u.contains("MIEN BAC")) return "MB";
        if (u.startsWith("MT") || u.contains("MIEN TRUNG")) return "MT";
        if (u.startsWith("MN") || u.contains("MIEN NAM")) return "MN";

        String token = canonicalProvince(u);
        for (String key : List.of("MB", "MT", "MN")) {
            if (sets.get(key).contains(token)) return key;
        }
        return u;
    }

    private static String display(String code) {
        return switch (code) {
            case "MB" -> "MIỀN BẮC";
            case "MT" -> "MIỀN TRUNG";
            case "MN" -> "MIỀN NAM";
            default -> code;
        };
    }

    // ===== DTO nhỏ để nhét vào chiTietTrung =====
    public static class WinDetail {
        public String dai;
        public String cachDanh;
        public String soDanh;
        public String giai;
        public Double tienTrung;

        public WinDetail(String dai, String cachDanh, String soDanh, String giai, Double tienTrung) {
            this.dai = dai;
            this.cachDanh = cachDanh;
            this.soDanh = soDanh;
            this.giai = giai;
            this.tienTrung = tienTrung;
        }
    }

    // =======================================================================
    //  CHÍNH: chạy & lưu kết quả tịch cho 1 người chơi
    // =======================================================================
    @Transactional
    public List<KetQuaTich> runAndSaveForPlayer(Long playerId, String playerName, LocalDate ngay) {

        Map<String, Set<String>> sets = scheduleSets(ngay);

        // (1) Tổng trúng
        Map<String, BigDecimal> tienTrungByCode = new HashMap<>();
        TongTienTrungDto trung = tongTienTrungService.tongHopTuDb(playerId, ngay);

        if (trung != null && trung.getCacMien() != null) {
            for (TongTienTrungDto.MienDto m : trung.getCacMien()) {
                String code = toCode(m.getMien(), sets);
                if (code.equals("MB") || code.equals("MT") || code.equals("MN")) {
                    tienTrungByCode.merge(code, bd(m.getTongTienMien()), BigDecimal::add);
                }
            }
        }

        // (2) Hoa hồng + Lớn / Nhỏ
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

        // (3) Tiền đánh
        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
        BigDecimal mb = BigDecimal.ZERO, mt = BigDecimal.ZERO, mn = BigDecimal.ZERO;

        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);
        for (var so : soList) {
            BigDecimal stake = parseTienDanh(so.getSoTien());
            String code = toCode(so.getMien(), sets);
            if (code.equals("MB")) mb = mb.add(stake);
            if (code.equals("MT")) mt = mt.add(stake);
            if (code.equals("MN")) mn = mn.add(stake);
        }
        tienDanhByCode.put("MB", mb);
        tienDanhByCode.put("MT", mt);
        tienDanhByCode.put("MN", mn);

        // Tên người chơi
        String resolvedName = resolvePlayerName(playerId, playerName, hhln, soList);

        // Snapshot cũ
        List<KetQuaTich> existedRows = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
        Map<String, KetQuaTich> existedByCode = new HashMap<>();
        for (KetQuaTich r : existedRows) {
            if (r.getMienCode() != null) existedByCode.put(r.getMienCode(), r);
        }

        // 🔵 Lấy TẤT CẢ bản ghi TRÚNG (summary=false) của player trong ngày
        List<KetQuaNguoiChoi> tatCaTrungTrongNgay =
                ketQuaNguoiChoiRepo.findChiTietTrungByPlayerAndNgay(playerId, ngay);

        // Build 3 miền
        List<KetQuaTich> rows = new ArrayList<>();

        for (String code : new String[]{"MB", "MT", "MN"}) {
            String display = display(code);

            BigDecimal tienTrung = tienTrungByCode.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienHH = hhBy.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienDanh = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienLN = lnBy.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienAT = tienTrung.add(tienHH).subtract(tienDanh);

            // 🔵 Build JSON chi tiết trúng cho riêng miền này
            String jsonChiTiet = buildChiTietJsonForRegion(tatCaTrungTrongNgay, code, sets);

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
                    .tienDanhDaNhanHoaHong(tienHH)
                    .tienDanhDaNhanHoaHongCongLonNho(
                            hhCongLnBy.getOrDefault(code, BigDecimal.ZERO))
                    .chiTietTrung(jsonChiTiet)
                    .build();

            KetQuaTich old = existedByCode.get(code);
            if (old != null) {
                entity.setId(old.getId());
                entity.setVersion(old.getVersion());
                entity.setCreatedAt(old.getCreatedAt());
            }

            rows.add(entity);
        }

        return ketQuaTichRepo.saveAll(rows);
    }

    // ==================== Helper Methods ======================

    private String resolvePlayerName(Long playerId, String playerName,
                                     TongHopHoaHongLonNhoDto hhln, List<Bet> soList) {
        String name = playerName;
        if (isBlank(name) && hhln != null && !isBlank(hhln.getPlayerName()))
            name = hhln.getPlayerName();
        if (isBlank(name))
            name = playerRepository.findById(playerId).map(Player::getName).orElse(null);
        if (isBlank(name) && !soList.isEmpty() && soList.get(0).getPlayer() != null)
            name = soList.get(0).getPlayer().getName();
        return name;
    }

    // Build JSON chi tiết cho từng miền
    private String buildChiTietJsonForRegion(List<KetQuaNguoiChoi> all,
                                             String code,
                                             Map<String, Set<String>> sets) {
        try {
            if (all == null || all.isEmpty()) return "[]";

            List<WinDetail> list = new ArrayList<>();
            for (KetQuaNguoiChoi k : all) {
                String codeOfRow = toCode(k.getMien(), sets);
                if (!code.equals(codeOfRow)) continue;          // khác miền → bỏ

                // chỉ lấy bản trúng (trung = true)
                if (Boolean.FALSE.equals(k.getTrung())) continue;

                Double tien = k.getTienTrung() != null ? k.getTienTrung() : 0d;

                list.add(new WinDetail(
                        k.getTenDai(),
                        k.getCachDanh(),
                        k.getSoDanh(),
                        k.getGiaiTrung(),
                        tien
                ));
            }

            if (list.isEmpty()) return "[]";
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Lỗi buildChiTietJsonForRegion", e);
            return "[]";
        }
    }

    private static BigDecimal parseTienDanh(String s) {
        if (s == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.replaceAll("[,\\s]", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean isBlank(String x) {
        return x == null || x.trim().isEmpty();
    }

    private static BigDecimal bd(Object x) {
        if (x == null) return BigDecimal.ZERO;
        if (x instanceof BigDecimal b) return b;
        if (x instanceof Double d) return BigDecimal.valueOf(d);
        return new BigDecimal(x.toString());
    }
}
