package com.example.doxoso.service;

import com.example.doxoso.model.TongHopHoaHongLonNhoDto;
import com.example.doxoso.model.TongTienAnThuaMien;
import com.example.doxoso.model.TongTienTrungDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TongTienAnThuaMienService {

    private final TongTienTrungService tongTienTrungService;
    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;

    // ⚠️ Đã bỏ TongTienDanhTheoMienService vì công thức của bạn không cần trừ vốn

    /**
     * Tính Ăn/Thua theo từng miền:
     * Công thức yêu cầu:
     * Tổng tiền ăn thua = Tổng tiền trúng - Tổng tiền hoa hồng
     *
     * Ví dụ:
     * Trúng = 0
     * Hoa hồng = 559
     * => Ăn thua = 0 - 559 = -559
     */
    public List<TongTienAnThuaMien> tinh(Long playerId, String playerName, LocalDate ngay) {

        // (1) Lấy tổng TRÚNG theo miền
        TongTienTrungDto tongTrungDto = tongTienTrungService.tongHopTuDb(playerId, ngay);
        Map<String, BigDecimal> tongTrungTheoMien = new HashMap<>();
        if (tongTrungDto != null && tongTrungDto.getCacMien() != null) {
            for (TongTienTrungDto.MienDto m : tongTrungDto.getCacMien()) {
                tongTrungTheoMien.put(safeStr(m.getMien()), safeBD(m.getTongTienMien()));
            }
        }

        // (2) Lấy HH + LỚN/NHỎ theo miền
        TongHopHoaHongLonNhoDto hhln =
                tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);

        Map<String, BigDecimal> hhBy = new HashMap<>();
        Map<String, BigDecimal> lnBy = new HashMap<>();

        if (hhln != null) {
            hhBy.put("MB", safeBD(hhln.getTongDaNhanHoaHongMB()));
            hhBy.put("MT", safeBD(hhln.getTongDaNhanHoaHongMT()));
            hhBy.put("MN", safeBD(hhln.getTongDaNhanHoaHongMN()));

            lnBy.put("MB", safeBD(hhln.getTienLonNhoMB()));
            lnBy.put("MT", safeBD(hhln.getTienLonNhoMT()));
            lnBy.put("MN", safeBD(hhln.getTienLonNhoMN()));
        }

        // (3) Tính toán
        List<TongTienAnThuaMien> out = new ArrayList<>();
        String[] order = new String[]{"MB", "MT", "MN"};

        for (String code : order) {
            String display = displayMien(code);

            BigDecimal tongTrung = tongTrungTheoMien.getOrDefault(display, BigDecimal.ZERO);
            BigDecimal tongHH = hhBy.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal lonNho = lnBy.getOrDefault(code, BigDecimal.ZERO);

            // ============================
            // CÔNG THỨC BẠN YÊU CẦU:
            // Ăn Thua = Trúng - Hoa Hồng
            // ============================
            BigDecimal antua = tongTrung.subtract(tongHH);

            out.add(TongTienAnThuaMien.builder()
                    .mien(display)
                    .tongTrung(tongTrung)
                    .tongHH(tongHH)
                    .lonNho(lonNho)
                    .tongAnThua(antua)
                    .build());
        }

        return out;
    }

    // ===== Helpers =====
    private static String displayMien(String code) {
        if ("MB".equals(code)) return "MIỀN BẮC";
        if ("MT".equals(code)) return "MIỀN TRUNG";
        if ("MN".equals(code)) return "MIỀN NAM";
        return code;
    }

    private static BigDecimal safeBD(Object x) {
        if (x == null) return BigDecimal.ZERO;
        if (x instanceof BigDecimal bd) return bd;
        if (x instanceof Double d) return BigDecimal.valueOf(d);
        return new BigDecimal(x.toString());
    }

    private static String safeStr(Object x) {
        return x == null ? "" : x.toString().trim();
    }
}