package com.example.doxoso.service;

import com.example.doxoso.model.Player;
import com.example.doxoso.model.PlayerTongTienHH;
import com.example.doxoso.model.PlayerTongTienDanhTheoMienDto;
import com.example.doxoso.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TongTienHHService {

    // Số lẻ muốn hiển thị (VND => có thể để 2, FE format vẫn OK)
    private static final int SCALE = 2;
    // Workaround thay cho RoundingMode.HALF_UP
    private static final int RM = BigDecimal.ROUND_HALF_UP;

    private final PlayerRepository playerRepository;
    private final TongTienDanhTheoMienService tongTienService;

    /* ========== API sẵn có: 1 player ========== */
    @Transactional(readOnly = true)
    public PlayerTongTienHH tinhHoaHongTheoMien(Long playerId) {
        PlayerTongTienDanhTheoMienDto tong = tongTienService.tinhTongTheoMien(playerId);

        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Player id=" + playerId));

        // 👇 LẤY % hoa hồng từ players, convert sang hệ số
        BigDecimal rate = percentToRate(p.getHoaHong());   // ví dụ 69.5 -> 0.695

        BigDecimal mb = safe(tong.getMienBac()).multiply(rate).setScale(SCALE, RM);
        BigDecimal mt = safe(tong.getMienTrung()).multiply(rate).setScale(SCALE, RM);
        BigDecimal mn = safe(tong.getMienNam()).multiply(rate).setScale(SCALE, RM);
        BigDecimal tongDaNhan = safe(tong.getTong()).multiply(rate).setScale(SCALE, RM);

        return PlayerTongTienHH.builder()
                .playerId(playerId)
                .playerName(tong.getPlayerName())
                .heSoHoaHong(rate)                // 69.5% -> 0.695
                .hoaHongMB(mb)
                .hoaHongMT(mt)
                .hoaHongMN(mn)
                .tongDaNhanHoaHong(tongDaNhan)
                .build();
    }

    /* ========== MỚI #1: Lấy tất cả playerId ========== */
    @Transactional(readOnly = true)
    public List<Long> getAllPlayerIds() {
        return playerRepository.findAllIds();
    }

    /* ========== MỚI #2: Tính hoa hồng cho TẤT CẢ player ========== */
    @Transactional(readOnly = true)
    public List<PlayerTongTienHH> tinhHoaHongTatCaPlayer() {
        // a) Lấy tổng theo miền của tất cả player (đã loại LỚN/NHỎ/LỚN-NHỎ nếu anh set EXCLUDED)
        List<PlayerTongTienDanhTheoMienDto> tongAll = tongTienService.tinhTatCaPlayer();

        // b) Lấy rate của tất cả player cần tính (1 lần) -> map {id -> Player}
        List<Long> ids = tongAll.stream().map(PlayerTongTienDanhTheoMienDto::getPlayerId).toList();
        Map<Long, Player> playerById = playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        // c) Duyệt và build DTO hoa hồng cho từng player
        List<PlayerTongTienHH> result = new ArrayList<>();
        for (PlayerTongTienDanhTheoMienDto t : tongAll) {
            Player p = playerById.get(t.getPlayerId());
            if (p == null) continue;

            BigDecimal rate = percentToRate(p.getHoaHong());   // 👈 dùng % từ players

            BigDecimal mb = safe(t.getMienBac()).multiply(rate).setScale(SCALE, RM);
            BigDecimal mt = safe(t.getMienTrung()).multiply(rate).setScale(SCALE, RM);
            BigDecimal mn = safe(t.getMienNam()).multiply(rate).setScale(SCALE, RM);
            BigDecimal tongDaNhan = safe(t.getTong()).multiply(rate).setScale(SCALE, RM);

            result.add(PlayerTongTienHH.builder()
                    .playerId(t.getPlayerId())
                    .playerName(t.getPlayerName())
                    .heSoHoaHong(rate)
                    .hoaHongMB(mb)
                    .hoaHongMT(mt)
                    .hoaHongMN(mn)
                    .tongDaNhanHoaHong(tongDaNhan)
                    .build());
        }

        // d) Sắp xếp giảm dần theo tổng hoa hồng (tuỳ chọn)
        result.sort(Comparator.comparing(PlayerTongTienHH::getTongDaNhanHoaHong).reversed());
        return result;
    }

    /** 👉 Tính hoa hồng *theo ngày* cho 1 player trong khoảng [from, to] */
    @Transactional(readOnly = true)
    public List<PlayerTongTienHH> tinhHoaHongTheoNgay(Long playerId, LocalDate from, LocalDate to) {
        // 1) Lấy tổng tiền theo MIỀN cho từng ngày
        List<PlayerTongTienDanhTheoMienDto> tongByDay = tongTienService.tinhTongTheoMienTheoNgay(playerId, from, to);

        // 2) Lấy % hoa hồng của player, convert sang hệ số
        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Player id=" + playerId));
        BigDecimal rate = percentToRate(p.getHoaHong());       // 👈 hoa hồng %

        // 3) Nhân theo từng ngày
        List<PlayerTongTienHH> rs = new ArrayList<>();
        for (PlayerTongTienDanhTheoMienDto d : tongByDay) {
            BigDecimal mb  = safe(d.getMienBac()).multiply(rate).setScale(SCALE, RM);
            BigDecimal mt  = safe(d.getMienTrung()).multiply(rate).setScale(SCALE, RM);
            BigDecimal mn  = safe(d.getMienNam()).multiply(rate).setScale(SCALE, RM);
            BigDecimal sum = safe(d.getTong()).multiply(rate).setScale(SCALE, RM);

            rs.add(PlayerTongTienHH.builder()
                    .playerId(playerId)
                    .playerName(d.getPlayerName())
                    .ngay(d.getNgay())
                    .heSoHoaHong(rate)
                    .hoaHongMB(mb)
                    .hoaHongMT(mt)
                    .hoaHongMN(mn)
                    .tongDaNhanHoaHong(sum)
                    .build());
        }
        return rs;
    }

    /* ================= Helpers ================= */

    // ✅ Nhận "5", "10", "69,5", "69.5", BigDecimal, Number...
    //    LUÔN hiểu là PHẦN TRĂM → convert sang hệ số 0.xx
    private static BigDecimal percentToRate(Object rawPercent) {
        if (rawPercent == null) return BigDecimal.ZERO;

        BigDecimal v;
        if (rawPercent instanceof BigDecimal) {
            v = (BigDecimal) rawPercent;
        } else if (rawPercent instanceof Number) {
            v = new BigDecimal(rawPercent.toString());
        } else {
            String s = rawPercent.toString().trim();
            if (s.isEmpty()) return BigDecimal.ZERO;

            // bỏ ký tự % và khoảng trắng
            s = s.replace("%", "").trim();

            // xử lý "69,5" -> "69.5"
            if (s.contains(",") && !s.contains(".")) {
                s = s.replace(".", "").replace(",", ".");
            } else {
                // "1,000.5" -> "1000.5"
                s = s.replace(",", "");
            }

            try {
                v = new BigDecimal(s);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Hoa hồng không hợp lệ: '" + rawPercent + "'");
            }
        }

        // LUÔN coi là % → chia 100
        // 5 -> 0.05 ; 69.5 -> 0.695
        return v.divide(BigDecimal.valueOf(100), 6, RM);
    }

    private static BigDecimal safe(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }
}
