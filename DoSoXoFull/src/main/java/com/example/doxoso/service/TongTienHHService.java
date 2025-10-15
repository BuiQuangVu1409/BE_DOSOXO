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

    // Số lẻ muốn hiển thị (VND => 0)
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

        BigDecimal rate = normalizeRate(p.getHoaHong()); // 69 -> 0.69 ; 69,5 -> 0.695 ; 0.05 -> 0.05

        BigDecimal mb = safe(tong.getMienBac()).multiply(rate).setScale(SCALE, RM);
        BigDecimal mt = safe(tong.getMienTrung()).multiply(rate).setScale(SCALE, RM);
        BigDecimal mn = safe(tong.getMienNam()).multiply(rate).setScale(SCALE, RM);
        BigDecimal tongDaNhan = safe(tong.getTong()).multiply(rate).setScale(SCALE, RM);

        return PlayerTongTienHH.builder()
                .playerId(playerId)
                .playerName(tong.getPlayerName())
                .heSoHoaHong(rate)
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
        // a) Lấy tổng theo miền của tất cả player (đã loại LỚN/NHỎ/LỚN-NHỎ)
        List<PlayerTongTienDanhTheoMienDto> tongAll = tongTienService.tinhTatCaPlayer();

        // b) Lấy rate của tất cả player cần tính (1 lần) -> map {id -> Player}
        List<Long> ids = tongAll.stream().map(PlayerTongTienDanhTheoMienDto::getPlayerId).toList();
        Map<Long, Player> playerById = playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        // c) Duyệt và build DTO hoa hồng cho từng player
        List<PlayerTongTienHH> result = new ArrayList<>();
        for (PlayerTongTienDanhTheoMienDto t : tongAll) {
            Player p = playerById.get(t.getPlayerId());
            if (p == null) continue; // không tìm thấy player tương ứng (hiếm)

            BigDecimal rate = normalizeRate(p.getHoaHong());

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

        // 2) Lấy hệ số hoa hồng của player (chuẩn hoá về tỉ lệ 0.xx)
        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Player id=" + playerId));
        BigDecimal rate = normalizeRate(p.getHoaHong());

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

    // Nhận BigDecimal/Number/String ("69", "69,5", "0.05") -> trả về tỉ lệ 0.xx
    private static BigDecimal normalizeRate(Object raw) {
        if (raw == null) return BigDecimal.ZERO;

        BigDecimal v;
        if (raw instanceof BigDecimal) {
            v = (BigDecimal) raw;
        } else if (raw instanceof Number) {
            v = new BigDecimal(raw.toString());
        } else {
            String s = raw.toString().trim();
            if (s.isEmpty()) return BigDecimal.ZERO;

            if (s.contains(",") && !s.contains(".")) {
                int comma = s.lastIndexOf(',');
                int decimals = s.length() - comma - 1;
                if (decimals >= 1 && decimals <= 3) {
                    s = s.replace(",", ".");
                } else {
                    s = s.replace(",", ""); // ngăn nghìn
                }
            } else {
                s = s.replace(",", ""); // ngăn nghìn
            }
            try {
                v = new BigDecimal(s);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Hoa hồng không hợp lệ: '" + raw + "'");
            }
        }

        // >1 => hiểu là % -> chia 100 về tỉ lệ; <=1 => giữ nguyên
        return (v.compareTo(BigDecimal.ONE) > 0)
                ? v.divide(BigDecimal.valueOf(100), 6, RM)
                : v.setScale(6, RM);
    }

    private static BigDecimal safe(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }
}
