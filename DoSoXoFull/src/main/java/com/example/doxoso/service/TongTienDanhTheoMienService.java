package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.PlayerTongTienDanhTheoMienDto;
import com.example.doxoso.model.Bet;
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

    // Loại trừ các cách đánh này
    private static final Set<String> EXCLUDED = Set.of("LON", "NHO", "LON NHO");

    /** ------------ TÍNH CHO 1 PLAYER (TRẢ VỀ DTO) ------------ */
    public PlayerTongTienDanhTheoMienDto tinhTongTheoMien(Long playerId) {
        // fetch theo player.id
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
        // dùng fetch join để tránh N+1 (hãy dùng findAllWithPlayer trong repository)
        List<Bet> all = betRepository.findAllWithPlayer();

        // group theo player.id (null-safe)
        Map<Long, List<Bet>> byPlayer = all.stream()
                .filter(s -> s.getPlayer() != null && s.getPlayer().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));

        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Bet>> e : byPlayer.entrySet()) {
            Long pid = e.getKey();
            CalcResult calc = tinhTuDanhSachSo(e.getValue());

            result.add(PlayerTongTienDanhTheoMienDto.builder()
                    .playerId(pid)
                    .playerName(calc.playerName) // có thể null nếu Player không có name
                    .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
                    .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
                    .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
                    .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
                    .build());
        }

        // (tuỳ chọn) sắp xếp theo tổng giảm dần
        result.sort(Comparator.comparing(PlayerTongTienDanhTheoMienDto::getTong).reversed());
        return result;
    }

    /** Tính từ 1 danh sách SoNguoiChoi của cùng player */
    private CalcResult tinhTuDanhSachSo(List<Bet> danhSach) {
        Map<String, BigDecimal> byRegion = new LinkedHashMap<>();
        byRegion.put("MIEN BAC", BigDecimal.ZERO);
        byRegion.put("MIEN TRUNG", BigDecimal.ZERO);
        byRegion.put("MIEN NAM", BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;
        String playerName = null;

        for (Bet so : danhSach) {
            // lấy tên player nếu có (ưu tiên getter chuẩn)
            if (playerName == null && so.getPlayer() != null) {
                try {
                    // Đổi "getName" nếu entity Player dùng field khác (vd: getUsername)
                    Object n = so.getPlayer().getClass().getMethod("getName").invoke(so.getPlayer());
                    if (n != null) playerName = String.valueOf(n);
                } catch (ReflectiveOperationException ignored) {}
            }

            String cachDanh = norm(so.getCachDanh());
            if (EXCLUDED.contains(cachDanh)) continue; // bỏ LỚN/NHỎ/LỚN NHỎ

            String mien = normMien(so.getMien());
            BigDecimal tien = parseTienDanh(so.getSoTien());

            // Nếu muốn nhân theo số đài người chơi chọn, bật đoạn dưới:
            // if (so.getTenDai() != null && so.getTenDai().contains(",")) {
            //     long soDai = Arrays.stream(so.getTenDai().split(","))
            //             .map(String::trim).filter(s -> !s.isEmpty()).count();
            //     tien = tien.multiply(BigDecimal.valueOf(soDai));
            // }

            if (byRegion.containsKey(mien)) {
                byRegion.put(mien, byRegion.get(mien).add(tien));
            } else {
                // nếu miền không khớp 3 nhóm chính, vẫn cộng dồn theo key thô
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
        // Lấy toàn bộ vé của player trong khoảng ngày
        List<Bet> all = betRepository.findByPlayer_IdAndNgayBetween(playerId, from, to);

        // Group theo ngày
        Map<LocalDate, List<Bet>> byDate = all.stream()
                .collect(Collectors.groupingBy(Bet::getNgay, TreeMap::new, Collectors.toList()));

        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();

        // Tái sử dụng logic cộng tiền hiện có (tinhTuDanhSachSo)
        for (Map.Entry<LocalDate, List<Bet>> e : byDate.entrySet()) {
            LocalDate ngay = e.getKey();
            var calc = tinhTuDanhSachSo(e.getValue()); // trả {playerName, map "MIEN ..."}

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
        return result; // đã theo thứ tự ngày tăng dần nhờ TreeMap
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
        if (n.contains("BAC")) return "MIEN BAC";
        if (n.contains("TRUNG")) return "MIEN TRUNG";
        if (n.contains("NAM")) return "MIEN NAM";
        return n; // fallback
    }

    private static BigDecimal parseTienDanh(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        String cleaned = s.replace(",", "").trim(); // bỏ dấu phân cách nghìn nếu có

        if (cleaned.contains("-")) {
            BigDecimal sum = BigDecimal.ZERO;
            for (String part : cleaned.split("-")) {
                if (!part.isBlank()) sum = sum.add(new BigDecimal(part.trim()));
            }
            return sum;
        }
        return new BigDecimal(cleaned);
    }

    private record CalcResult(String playerName, Map<String, BigDecimal> mapTheoMien) {}
}
