package com.example.doxoso.service;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.BetRepository;
import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
import com.example.doxoso.repository.KetQuaTichRepository;
import com.example.doxoso.repository.PlayerRepository;
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

    private final TongTienTrungService tongTienTrungService;              // hiện giờ ít dùng, giữ lại để sau
    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;
    private final TongTienAnThuaMienService tongTienAnThuaMienService;    // hiện chưa dùng, để sẵn
    private final LichQuayXoSoService lichQuayXoSoService;
    private final KetQuaService ketQuaService;

    // 👉 Tổng tiền đánh theo miền (KHÔNG LỚN/NHỎ, đã nhân số đài)
    private final TongTienDanhTheoMienService tongTienDanhTheoMienService;

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
            for (String province : list) {
                target.add(canonicalProvince(province));
            }
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

        // 0. Lấy tất cả BET của player trong ngày
        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);

        // 0.1 XÓA TOÀN BỘ KQ NGƯỜI CHƠI cũ (mọi miền/đài) của player trong ngày
        ketQuaNguoiChoiRepo.deleteByPlayerIdAndNgayChoi(playerId, ngay);

        // 0.2 Nếu KHÔNG còn BET nào → xoá luôn KQ TỊCH & trả về rỗng
        if (soList.isEmpty()) {
            ketQuaTichRepo.deleteByPlayerIdAndNgay(playerId, ngay);
            return List.of();
        }

        // 0.3 Nếu còn BET → DÒ LẠI & LƯU KQ NGƯỜI CHƠI
        ketQuaService.doKetQua(soList); // bên trong đã gọi ketQuaNguoiChoiService.luuKetQua(bet, dto)

        // 1. Lịch mở thưởng theo miền (để map tên đài -> code MB/MT/MN)
        Map<String, Set<String>> sets = scheduleSets(ngay);

        // 2. Hoa hồng + LỚN/NHỎ (tổng hợp cũ) – giờ chỉ dùng để hỗ trợ lấy tên player nếu cần
        TongHopHoaHongLonNhoDto hhln =
                tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);

        // 3. Tiền LỚN / NHỎ theo miền (từ BET) – CHỈ TIỀN ĐÁNH (để hiển thị @Transient)
        Map<String, BigDecimal> tienLonByCode = new HashMap<>();
        Map<String, BigDecimal> tienNhoByCode = new HashMap<>();

        for (Bet so : soList) {
            BigDecimal stake = parseTienDanh(so.getSoTien());
            String code = toCode(so.getMien(), sets);

            // Chỉ quan tâm 3 miền chuẩn
            if (!"MB".equals(code) && !"MT".equals(code) && !"MN".equals(code)) {
                continue;
            }

            String cach = normalizeNoAccent(so.getCachDanh());
            if (cach.contains("LON")) {
                tienLonByCode.merge(code, stake, BigDecimal::add);
            }
            if (cach.contains("NHO")) {
                tienNhoByCode.merge(code, stake, BigDecimal::add);
            }
        }

        // 4. Tiền ĐÁNH THEO MIỀN (KHÔNG LỚN/NHỎ, đã nhân 2/3 đài) từ TongTienDanhTheoMienService
        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
        List<PlayerTongTienDanhTheoMienDto> tongList =
                tongTienDanhTheoMienService.tinhTongTheoMienTheoNgay(playerId, ngay, ngay);

        if (!tongList.isEmpty()) {
            PlayerTongTienDanhTheoMienDto dto = tongList.get(0);
            tienDanhByCode.put("MB",
                    Optional.ofNullable(dto.getMienBac()).orElse(BigDecimal.ZERO));
            tienDanhByCode.put("MT",
                    Optional.ofNullable(dto.getMienTrung()).orElse(BigDecimal.ZERO));
            tienDanhByCode.put("MN",
                    Optional.ofNullable(dto.getMienNam()).orElse(BigDecimal.ZERO));
        }

        // 5. Tên người chơi
        String resolvedName = resolvePlayerName(playerId, playerName, hhln, soList);

        // 5.1. Hoa hồng % của player – lấy từ bảng players
        Double hoaHongPlayer = playerRepository.findById(playerId)
                .map(Player::getHoaHong)
                .orElse(null);

        // 6. Snapshot KQTICH cũ để giữ id/version/createdAt nếu có
        List<KetQuaTich> existedRows = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
        Map<String, KetQuaTich> existedByCode = new HashMap<>();
        for (KetQuaTich r : existedRows) {
            if (r.getMienCode() != null) {
                existedByCode.put(r.getMienCode(), r);
            }
        }

        // 7. Lấy TOÀN BỘ kết quả người chơi trong ngày (cả trúng lẫn trật)
        List<KetQuaNguoiChoi> ketQuaTrongNgay =
                ketQuaNguoiChoiRepo.findByPlayerIdAndNgayChoi(playerId, ngay);

        // 7.1. Tách tiền trúng THƯỜNG + NET LỚN / NHỎ theo miền
        Map<String, BigDecimal> tienTrungThuongByCode = new HashMap<>();
        Map<String, BigDecimal> tienLonNetByCode      = new HashMap<>();
        Map<String, BigDecimal> tienNhoNetByCode      = new HashMap<>();

        if (ketQuaTrongNgay != null) {
            for (KetQuaNguoiChoi k : ketQuaTrongNgay) {
                String codeOfRow = toCode(k.getMien(), sets);
                if (!"MB".equals(codeOfRow) && !"MT".equals(codeOfRow) && !"MN".equals(codeOfRow)) {
                    continue;
                }

                String cachNorm = normalizeNoAccent(k.getCachDanh());
                boolean laLon   = cachNorm.contains("LON");
                boolean laNho   = cachNorm.contains("NHO");
                boolean laLonNho = laLon || laNho;

                BigDecimal tienTrung = bd(k.getTienTrung());
                BigDecimal tienDanh  = bd(k.getTienDanh());

                // 👉 Kèo THƯỜNG
                if (!laLonNho) {
                    if (Boolean.TRUE.equals(k.getTrung()) &&
                            tienTrung.compareTo(BigDecimal.ZERO) > 0) {
                        tienTrungThuongByCode.merge(codeOfRow, tienTrung, BigDecimal::add);
                    }
                    continue;
                }

                // 👉 Kèo LỚN / NHỎ:
                //    - trúng  → + tiềnTrung
                //    - trật   → - tiềnDanh
                BigDecimal delta;
                if (Boolean.TRUE.equals(k.getTrung()) &&
                        tienTrung.compareTo(BigDecimal.ZERO) > 0) {
                    delta = tienTrung;
                } else {
                    delta = tienDanh.negate();
                }

                if (laLon) {
                    tienLonNetByCode.merge(codeOfRow, delta, BigDecimal::add);
                }
                if (laNho) {
                    tienNhoNetByCode.merge(codeOfRow, delta, BigDecimal::add);
                }
            }
        }

        // 8. Build 3 miền
        List<KetQuaTich> rows = new ArrayList<>();

        for (String code : new String[]{"MB", "MT", "MN"}) {
            String display = display(code);

            // 8.1. Tiền trúng THƯỜNG theo miền (đã tách riêng, không có LỚN/NHỎ)
            BigDecimal tienTrungThuong = tienTrungThuongByCode.getOrDefault(code, BigDecimal.ZERO);

            // 8.2. Tiền ĐÁNH theo miền (KHÔNG LỚN/NHỎ, đã nhân số đài)
            BigDecimal tienDanh = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);

            // 8.3. Tổng tiền ĐÁNH LỚN / ĐÁNH NHỎ theo miền (TIỀN ĐÁNH – chỉ dùng hiển thị)
            BigDecimal tienLonDanh = tienLonByCode.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienNhoDanh = tienNhoByCode.getOrDefault(code, BigDecimal.ZERO);

            // 8.4. NET LỚN & NET NHỎ tách riêng
            BigDecimal tienLonNet = tienLonNetByCode
                    .getOrDefault(code, BigDecimal.ZERO)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal tienNhoNet = tienNhoNetByCode
                    .getOrDefault(code, BigDecimal.ZERO)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            // 8.5. NET LỚN/NHỎ tổng (dùng cho tổng 3 miền) = LỚN + NHỎ
            BigDecimal tienLonNhoNet = tienLonNet.add(tienNhoNet);

            // 8.6. Tính tiền hoa hồng: tiền đánh (KHÔNG LỚN/NHỎ) × % hoa hồng
            BigDecimal tienHH = BigDecimal.ZERO;
            if (hoaHongPlayer != null) {
                BigDecimal rate = bd(hoaHongPlayer)
                        .divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);
                tienHH = tienDanh
                        .multiply(rate)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
            }

            // 8.7. CÔNG THỨC: Tiền ăn/thua (THƯỜNG) = Tổng TRÚNG THƯỜNG - Tổng HOA HỒNG
            //       (KHÔNG trừ LỚN/NHỎ, vì đã tính riêng vào tienLonNhoNet)
            BigDecimal tienAT = tienTrungThuong
                    .subtract(tienHH)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            // 8.8. JSON chi tiết trúng (CHỈ kèo THƯỜNG, KHÔNG bao gồm LỚN/NHỎ)
            String jsonChiTiet = buildChiTietJsonForRegion(ketQuaTrongNgay, code, sets);

            KetQuaTich entity = KetQuaTich.builder()
                    .playerId(playerId)
                    .playerName(resolvedName)
                    .ngay(ngay)
                    .mienCode(code)
                    .mienDisplay(display)

                    .tienTrung(tienTrungThuong)                  // ✅ chỉ trúng THƯỜNG
                    .tienHoaHong(tienHH)                        // ✅ hoa hồng
                    .tienLonNho(tienLonNhoNet)                  // ✅ NET LỚN/NHỎ (LỚN + NHỎ)
                    .tienAnThua(tienAT)                         // ✅ ăn/thua THƯỜNG

                    .tienDanh(tienDanh)                         // ✅ tổng tiền đánh (không L/N)
                    .tienDanhDaNhanHoaHong(tienHH)
                    .tienDanhDaNhanHoaHongCongLonNho(
                            tienHH.add(tienLonNhoNet)           // thông tin thêm: hoa hồng + NET L/N
                    )
                    .chiTietTrung(jsonChiTiet)

                    // 👉 NET riêng LỚN / NHỎ (dùng cho FE)
                    .tienLonNet(tienLonNet)
                    .tienNhoNet(tienNhoNet)

                    .build();

            KetQuaTich old = existedByCode.get(code);
            if (old != null) {
                entity.setId(old.getId());
                entity.setVersion(old.getVersion());
                entity.setCreatedAt(old.getCreatedAt());
            }

            rows.add(entity);
        }

        // 9. Lưu DB
        List<KetQuaTich> saved = ketQuaTichRepo.saveAll(rows);

        // 🔥 10. GẮN LẠI CÁC FIELD @Transient CHO LIST TRẢ RA
        for (KetQuaTich kq : saved) {
            // % hoa hồng player (lấy từ bảng players)
            kq.setHoaHongPlayer(hoaHongPlayer);

            // TIỀN ĐÁNH LỚN / TIỀN ĐÁNH NHỎ + NET riêng theo miền (chỉ dùng hiển thị, không lưu DB)
            String code = kq.getMienCode();
            if (code != null) {
                kq.setTienLonDanh(tienLonByCode.getOrDefault(code, BigDecimal.ZERO));
                kq.setTienNhoDanh(tienNhoByCode.getOrDefault(code, BigDecimal.ZERO));
                kq.setTienLonNet(
                        tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO)
                                .setScale(2, BigDecimal.ROUND_HALF_UP)
                );
                kq.setTienNhoNet(
                        tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO)
                                .setScale(2, BigDecimal.ROUND_HALF_UP)
                );
            }
        }

        return saved;
    }

    // ==================== Helper Methods ======================

    private String resolvePlayerName(Long playerId, String playerName,
                                     TongHopHoaHongLonNhoDto hhln, List<Bet> soList) {
        String name = playerName;
        if (isBlank(name) && hhln != null && !isBlank(hhln.getPlayerName())) {
            name = hhln.getPlayerName();
        }
        if (isBlank(name)) {
            name = playerRepository.findById(playerId).map(Player::getName).orElse(null);
        }
        if (isBlank(name) && !soList.isEmpty() && soList.get(0).getPlayer() != null) {
            name = soList.get(0).getPlayer().getName();
        }
        return name;
    }

    // Build JSON chi tiết cho từng miền – CHỈ kèo THƯỜNG, bỏ LỚN/NHỎ
    private String buildChiTietJsonForRegion(List<KetQuaNguoiChoi> all,
                                             String code,
                                             Map<String, Set<String>> sets) {
        try {
            if (all == null || all.isEmpty()) return "[]";

            List<WinDetail> list = new ArrayList<>();
            for (KetQuaNguoiChoi k : all) {
                String codeOfRow = toCode(k.getMien(), sets);
                if (!code.equals(codeOfRow)) continue;          // khác miền → bỏ

                // Chỉ lấy bản TRÚNG
                if (Boolean.FALSE.equals(k.getTrung())) continue;

                // Bỏ LỚN / NHỎ ra khỏi chi tiết trúng (vì đã có hàng riêng)
                String cachNorm = normalizeNoAccent(k.getCachDanh());
                if (cachNorm.contains("LON") || cachNorm.contains("NHO")) {
                    continue;
                }

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

    // parse được cả 3 CHÂN kiểu "10000-20000-30000"
    private static BigDecimal parseTienDanh(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;

        String cleaned = s.replace(",", "").trim();

        if (cleaned.contains("-")) {
            BigDecimal sum = BigDecimal.ZERO;
            String[] parts = cleaned.split("-");
            for (String part : parts) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                try {
                    sum = sum.add(new BigDecimal(p));
                } catch (NumberFormatException ignored) {
                }
            }
            return sum;
        }

        try {
            return new BigDecimal(cleaned);
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
