package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.PlayerTongTienHH;
import com.example.doxoso.model.Bet;
import com.example.doxoso.model.TongHopHoaHongLonNhoDto;
import com.example.doxoso.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TongHopHoaHongLonNhoService {

    private final BetRepository betRepository;
    private final TongTienHHService tongTienHHService; // đã có sẵn bên bạn

    public TongHopHoaHongLonNhoDto tongHopMotNgay(Long playerId, String playerName, LocalDate ngay) {
        // 1) Lấy các số ĐÃ NHÂN HOA HỒNG cho 1 ngày từ TongTienHHService
        PlayerTongTienHH hh = layHoaHongMotNgay(playerId, ngay);

        BigDecimal mbBase = safe(hh == null ? null : hh.getHoaHongMB());
        BigDecimal mtBase = safe(hh == null ? null : hh.getHoaHongMT());
        BigDecimal mnBase = safe(hh == null ? null : hh.getHoaHongMN());
        BigDecimal tongBase = safe(hh == null ? null : hh.getTongDaNhanHoaHong());

        if ((playerName == null || playerName.isBlank()) && hh != null) {
            playerName = hh.getPlayerName();
        }

        // 2) Cộng LỚN/NHỎ theo đúng miền từ so_nguoi_choi
        List<Bet> items = betRepository.findByPlayerIdAndNgay(playerId, ngay);

        BigDecimal mbLN = BigDecimal.ZERO, mtLN = BigDecimal.ZERO, mnLN = BigDecimal.ZERO;

        for (Bet so : items) {
            String cd = safeUpper(so.getCachDanh());
            // chỉ giữ LỚN/NHỎ (có dấu & không dấu)
            if (!("LỚN".equals(cd) || "LON".equals(cd) || "NHỎ".equals(cd) || "NHO".equals(cd))) continue;

            BigDecimal stake = MoneyParser.parseToBigDecimal(so.getSoTien()); // bạn đang lưu String

            switch (mapMien(so.getMien())) {
                case "MB" -> mbLN = mbLN.add(stake);
                case "MT" -> mtLN = mtLN.add(stake);
                case "MN" -> mnLN = mnLN.add(stake);
                default -> { /* bỏ qua miền khác/không hợp lệ */ }
            }
        }

        // 3) Tổng cộng = base + LỚN/NHỎ
        BigDecimal mbTotal = mbBase.add(mbLN);
        BigDecimal mtTotal = mtBase.add(mtLN);
        BigDecimal mnTotal = mnBase.add(mnLN);

        BigDecimal tongLN  = mbLN.add(mtLN).add(mnLN);
        BigDecimal tongAll = mbTotal.add(mtTotal).add(mnTotal);

        return TongHopHoaHongLonNhoDto.builder()
                .playerId(playerId)
                .playerName(playerName)
                .ngay(ngay)

                .tongDaNhanHoaHong(tongBase)
                .tongDaNhanHoaHongMB(mbBase)
                .tongDaNhanHoaHongMT(mtBase)
                .tongDaNhanHoaHongMN(mnBase)

                .tienLonNhoMB(mbLN)
                .tienLonNhoMT(mtLN)
                .tienLonNhoMN(mnLN)
                .tongLonNho(tongLN)

                .tongCongMB(mbTotal)
                .tongCongMT(mtTotal)
                .tongCongMN(mnTotal)
                .tongCong(tongAll)
                .build();
    }

    /** Lấy hoa hồng của đúng 1 ngày (from=to) từ TongTienHHService */
    private PlayerTongTienHH layHoaHongMotNgay(Long playerId, LocalDate ngay) {
        var list = tongTienHHService.tinhHoaHongTheoNgay(playerId, ngay, ngay);
        return list.isEmpty() ? null : list.get(0);
    }

    // ===== Helpers =====
    private static BigDecimal safe(BigDecimal n) { return n == null ? BigDecimal.ZERO : n; }
    private static String safeUpper(String s) { return s == null ? "" : s.trim().toUpperCase(); }

    private static String mapMien(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toUpperCase();
        if (s.startsWith("MB") || s.contains("BẮC") || s.contains("BAC")) return "MB";
        if (s.startsWith("MT") || s.contains("TRUNG"))                  return "MT";
        if (s.startsWith("MN") || s.contains("NAM"))                    return "MN";
        return s;
    }


    public final class MoneyParser {
        public static BigDecimal parseToBigDecimal(String s) {
            if (s == null || s.isBlank()) return BigDecimal.ZERO;
            String cleaned = s.replaceAll("[,.\\s]", ""); // bỏ dấu . , khoảng trắng
            return new BigDecimal(cleaned);
        }
    }

}
