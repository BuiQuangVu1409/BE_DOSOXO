package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.PlayerTongTienDanhTheoMienDto;
import com.example.doxoso.repository.BetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TongTienDanhTheoMienService {

    private final BetRepository betRepository;

    // Loại trừ các cách đánh này (nếu sau này muốn tính luôn LỚN/NHỎ thì sửa lại set này)
    private static final Set<String> EXCLUDED = Set.of("LON", "NHO", "LON NHO");

    /** ------------ TÍNH CHO 1 PLAYER (TRẢ VỀ DTO) ------------ */
    public PlayerTongTienDanhTheoMienDto tinhTongTheoMien(Long playerId) {
        List<Bet> ds = betRepository.findByPlayer_Id(playerId);
        CalcResult calc = tinhTuDanhSachSo(ds);

        return PlayerTongTienDanhTheoMienDto.builder()
                .playerId(playerId)
                .playerName(calc.playerName)
                .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
                .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
                .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
                .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
                .build();
    }

    /** ------------ TÍNH CHO TẤT CẢ PLAYER (LIST DTO) ------------ */
    public List<PlayerTongTienDanhTheoMienDto> tinhTatCaPlayer() {
        List<Bet> all = betRepository.findAllWithPlayer();

        Map<Long, List<Bet>> byPlayer = all.stream()
                .filter(s -> s.getPlayer() != null && s.getPlayer().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));

        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Bet>> e : byPlayer.entrySet()) {
            Long pid = e.getKey();
            CalcResult calc = tinhTuDanhSachSo(e.getValue());

            result.add(PlayerTongTienDanhTheoMienDto.builder()
                    .playerId(pid)
                    .playerName(calc.playerName)
                    .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
                    .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
                    .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
                    .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
                    .build());
        }

        result.sort(Comparator.comparing(PlayerTongTienDanhTheoMienDto::getTong).reversed());
        return result;
    }

    /** Tính từ 1 danh sách Bet của cùng player */
    private CalcResult tinhTuDanhSachSo(List<Bet> danhSach) {
        Map<String, BigDecimal> byRegion = new LinkedHashMap<>();
        byRegion.put("MIEN BAC", BigDecimal.ZERO);
        byRegion.put("MIEN TRUNG", BigDecimal.ZERO);
        byRegion.put("MIEN NAM", BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;
        String playerName = null;

        for (Bet so : danhSach) {
            // Lấy tên player nếu có
            if (playerName == null && so.getPlayer() != null) {
                try {
                    Object n = so.getPlayer().getClass().getMethod("getName").invoke(so.getPlayer());
                    if (n != null) playerName = String.valueOf(n);
                } catch (ReflectiveOperationException ignored) {}
            }

            String cachDanh = norm(so.getCachDanh());
            if (EXCLUDED.contains(cachDanh)) continue; // hiện tại vẫn bỏ LỚN/NHỎ/LỚN NHỎ

            String mien = normMien(so.getMien());

            // 1) Lấy tiền trên 1 dòng BET
            BigDecimal tien = parseTienDanh(so.getSoTien());

            // 2) Nhân theo số đài thực tế (2 đài / 3 đài)
            long soDai = countSoDai(so); // <= helper bên dưới
            if (soDai > 1) {
                tien = tien.multiply(BigDecimal.valueOf(soDai));
            }

            // 3) Cộng vào map theo miền + tổng
            if (byRegion.containsKey(mien)) {
                byRegion.put(mien, byRegion.get(mien).add(tien));
            } else {
                byRegion.put(mien, tien);
            }
            grandTotal = grandTotal.add(tien);
        }

        byRegion.put("TONG", grandTotal);
        return new CalcResult(playerName, byRegion);
    }

    /** 👉 Tính tổng tiền theo *từng ngày* cho 1 player trong khoảng [from, to] */
    @Transactional
    public List<PlayerTongTienDanhTheoMienDto> tinhTongTheoMienTheoNgay(Long playerId, LocalDate from, LocalDate to) {
        List<Bet> all = betRepository.findByPlayer_IdAndNgayBetween(playerId, from, to);

        Map<LocalDate, List<Bet>> byDate = all.stream()
                .collect(Collectors.groupingBy(Bet::getNgay, TreeMap::new, Collectors.toList()));

        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Bet>> e : byDate.entrySet()) {
            LocalDate ngay = e.getKey();
            var calc = tinhTuDanhSachSo(e.getValue());

            result.add(PlayerTongTienDanhTheoMienDto.builder()
                    .playerId(playerId)
                    .playerName(calc.playerName)
                    .ngay(ngay)
                    .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
                    .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
                    .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
                    .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
                    .build());
        }
        return result;
    }

    // ===== Helpers =====

    private static String norm(String s) {
        if (s == null) return "";
        String noDia = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String upper = noDia.toUpperCase(Locale.ROOT).trim();
        return upper.replaceAll("\\s+", " ");
    }

    private static String normMien(String s) {
        String n = norm(s);
        if (n.isEmpty()) return "";

        if (n.equals("MB")) return "MIEN BAC";
        if (n.equals("MT")) return "MIEN TRUNG";
        if (n.equals("MN")) return "MIEN NAM";

        if (n.contains("BAC")) return "MIEN BAC";
        if (n.contains("TRUNG")) return "MIEN TRUNG";
        if (n.contains("NAM")) return "MIEN NAM";

        return n;
    }

    private static BigDecimal parseTienDanh(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;

        String cleaned = s.replace(".", "").replace(",", "").trim();

        try {
            if (cleaned.contains("-")) {
                BigDecimal sum = BigDecimal.ZERO;
                for (String part : cleaned.split("-")) {
                    String p = part.trim();
                    if (!p.isBlank()) {
                        sum = sum.add(new BigDecimal(p));
                    }
                }
                return sum;
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Đếm số đài từ field "dai" / "tenDai" của Bet.
     * - "3 đài"  → 3
     * - "2 đài"  → 2
     * - "Cà Mau" → 1
     * - "Cà Mau, TP.HCM, Đồng Tháp" → 3
     */
    private static long countSoDai(Bet so) {
        // TODO: nếu entity dùng getDai() thì đổi lại ở đây
        String raw = null;
        try {
            raw = (String) so.getClass().getMethod("getTenDai").invoke(so);
        } catch (Exception ignored) {
        }

        if (raw == null || raw.isBlank()) return 1L;

        // Nếu lưu dạng list "Cà Mau, TP.HCM, Đồng Tháp"
        if (raw.contains(",")) {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .count();
        }

        // Nếu lưu dạng "3 đài", "2 dai"
        String n = norm(raw); // => "3 DAI", "2 DAI", "HA NOI"
        if (n.contains("3 DAI")) return 3L;
        if (n.contains("2 DAI")) return 2L;

        // Mặc định 1 đài
        return 1L;
    }

    private record CalcResult(String playerName, Map<String, BigDecimal> mapTheoMien) {}
}
