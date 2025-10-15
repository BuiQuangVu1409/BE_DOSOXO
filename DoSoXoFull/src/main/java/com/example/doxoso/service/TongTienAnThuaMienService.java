// com.example.doxoso.service.AnThuaTheoMienService.java
package com.example.doxoso.service;

import com.example.doxoso.model.*;
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

    /**
     * Tính Ăn/Thua theo từng miền cho 1 player trong 1 ngày:
     *  - Lấy tổng TRÚNG theo miền từ TongTienTrungService
     *  - Lấy HH + LỚN/NHỎ theo miền từ TongHopHoaHongLonNhoService
     *  - Ăn/Thua = Trúng - (HH + Lớn/Nhỏ)
     */
    public List<TongTienAnThuaMien> tinh(Long playerId, String playerName, LocalDate ngay) {
        // (1) Lấy tổng TRÚNG theo miền
        TongTienTrungDto tongTrungDto = tongTienTrungService.tongHopTuDb(playerId, ngay);

        // Map tên hiển thị "MIỀN BẮC/TRUNG/NAM" -> tổng trúng
        Map<String, BigDecimal> tongTrungTheoMien = new LinkedHashMap<>();
        if (tongTrungDto != null && tongTrungDto.getCacMien() != null) {
            for (TongTienTrungDto.MienDto m : tongTrungDto.getCacMien()) {
                String ten = safeStr(m.getMien());                 // "MIỀN BẮC" ...
                BigDecimal val = safeBD(m.getTongTienMien());      // đã scale trong service kia
                tongTrungTheoMien.put(ten, val);
            }
        }

        // (2) Lấy HH + LỚN/NHỎ theo miền
        TongHopHoaHongLonNhoDto hhln = tongHopHoaHongLonNhoService
                .tongHopMotNgay(playerId, playerName, ngay);

        // Map code miền chuẩn (MB/MT/MN) → HH, LN
        Map<String, BigDecimal> hhBy = new HashMap<>();
        Map<String, BigDecimal> lnBy = new HashMap<>();

        hhBy.put("MB", safeBD(hhln.getTongDaNhanHoaHongMB()));
        hhBy.put("MT", safeBD(hhln.getTongDaNhanHoaHongMT()));
        hhBy.put("MN", safeBD(hhln.getTongDaNhanHoaHongMN()));

        lnBy.put("MB", safeBD(hhln.getTienLonNhoMB()));
        lnBy.put("MT", safeBD(hhln.getTienLonNhoMT()));
        lnBy.put("MN", safeBD(hhln.getTienLonNhoMN()));

        // (3) Ghép và tính Ăn/Thua theo thứ tự MB → MT → MN
        List<TongTienAnThuaMien> out = new ArrayList<>();

        String[] order = new String[] {"MB", "MT", "MN"};
        for (String code : order) {
            String display = displayMien(code);

            BigDecimal tongTrung = tongTrungTheoMien.getOrDefault(display, BigDecimal.ZERO);
            BigDecimal tongHH    = hhBy.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal lonNho    = lnBy.getOrDefault(code, BigDecimal.ZERO);

            BigDecimal antua = tongTrung.subtract(tongHH.add(lonNho));

            TongTienAnThuaMien item = TongTienAnThuaMien.builder()
                    .mien(display)
                    .tongTrung(tongTrung)
                    .tongHH(tongHH)
                    .lonNho(lonNho)
                    .tongAnThua(antua)
                    .build();

            out.add(item);
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
        if (x instanceof Double d)      return BigDecimal.valueOf(d);
        return new BigDecimal(x.toString());
    }
    private static String safeStr(Object x) {
        return x == null ? "" : x.toString().trim();
    }
}
