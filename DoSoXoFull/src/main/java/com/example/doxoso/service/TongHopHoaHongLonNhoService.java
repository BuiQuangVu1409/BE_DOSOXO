package com.example.doxoso.service;

import com.example.doxoso.model.KetQuaNguoiChoi;
import com.example.doxoso.model.PlayerTongTienHH;
import com.example.doxoso.model.TongHopHoaHongLonNhoDto;
import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TongHopHoaHongLonNhoService {

    private final TongTienHHService tongTienHHService;           // Hoa hồng theo ngày
    private final KetQuaNguoiChoiRepository ketQuaNguoiChoiRepo; // Kết quả LỚN/NHỎ đã tính ±

    /**
     * Tổng hợp 1 ngày cho 1 player:
     *  - Lấy hoa hồng MB/MT/MN từ TongTienHHService
     *  - Lấy tất cả kết quả LỚN/NHỎ trong ngày từ KetQuaNguoiChoi
     *      + tienTrung > 0: trúng LỚN/NHỎ (số dương, tính theo công thức)
     *      + tienTrung < 0: trật LỚN/NHỎ ( âm đúng số tiền đánh )
     *  - Cộng lại theo miền để ra net LỚN/NHỎ từng miền + tổng cộng
     */
    public TongHopHoaHongLonNhoDto tongHopMotNgay(Long playerId, String playerName, LocalDate ngay) {

        // 1) Hoa hồng base (đã "đánh xong" = tiền đánh đã nhân hoa hồng)
        PlayerTongTienHH hh = layHoaHongMotNgay(playerId, ngay);

        BigDecimal mbBase  = safe(hh == null ? null : hh.getHoaHongMB());
        BigDecimal mtBase  = safe(hh == null ? null : hh.getHoaHongMT());
        BigDecimal mnBase  = safe(hh == null ? null : hh.getHoaHongMN());
        BigDecimal tongBase = safe(hh == null ? null : hh.getTongDaNhanHoaHong());

        if ((playerName == null || playerName.isBlank()) && hh != null) {
            playerName = hh.getPlayerName();
        }

        // 2) Lấy tất cả kết quả của player trong ngày, lọc LỚN/NHỎ
        List<KetQuaNguoiChoi> kqList =
                ketQuaNguoiChoiRepo.findByPlayerIdAndNgayChoi(playerId, ngay);

        BigDecimal mbLN = BigDecimal.ZERO;
        BigDecimal mtLN = BigDecimal.ZERO;
        BigDecimal mnLN = BigDecimal.ZERO;

        for (KetQuaNguoiChoi kq : kqList) {
            if (!isLonNho(kq.getCachDanh())) continue;

            // ❗ tienTrung cho LỚN/NHỎ ở đây được hiểu là GIÁ TRỊ CÓ DẤU:
            //   - Trúng  => dương (theo công thức tính tiền trúng LỚN/NHỎ)
            //   - Trật   => âm   (bằng số tiền đánh, có dấu -)
            BigDecimal net = safe(kq.getTienTrung());

            switch (toMienCode(kq.getMien())) {
                case "MB" -> mbLN = mbLN.add(net);
                case "MT" -> mtLN = mtLN.add(net);
                case "MN" -> mnLN = mnLN.add(net);
                default -> {
                    // miền không hợp lệ thì bỏ qua
                }
            }
        }

        // 3) Tổng LỚN/NHỎ toàn bộ (có dấu)
        BigDecimal tongLN = mbLN.add(mtLN).add(mnLN);

        // 4) Tổng cộng = Hoa hồng + LỚN/NHỎ (đều đã là số ±)
        BigDecimal mbTotal = mbBase.add(mbLN);
        BigDecimal mtTotal = mtBase.add(mtLN);
        BigDecimal mnTotal = mnBase.add(mnLN);
        BigDecimal tongAll = mbTotal.add(mtTotal).add(mnTotal);

        return TongHopHoaHongLonNhoDto.builder()
                .playerId(playerId)
                .playerName(playerName)
                .ngay(ngay)

                // Hoa hồng đã nhận
                .tongDaNhanHoaHong(tongBase)
                .tongDaNhanHoaHongMB(mbBase)
                .tongDaNhanHoaHongMT(mtBase)
                .tongDaNhanHoaHongMN(mnBase)

                // LỚN / NHỎ (net ± theo miền)
                .tienLonNhoMB(mbLN)
                .tienLonNhoMT(mtLN)
                .tienLonNhoMN(mnLN)
                .tongLonNho(tongLN)

                // Tổng cộng (Hoa hồng + LỚN/NHỎ)
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

    // ================= Helpers =================

    private static BigDecimal safe(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }

    private static BigDecimal safe(Double d) {
        return d == null ? BigDecimal.ZERO : BigDecimal.valueOf(d);
    }

    private static String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    /**
     * Nhận diện cách đánh LỚN/NHỎ (bao gồm các biến thể:
     *  - LỚN, LỚN ĐẦU, LỚN ĐUÔI, LỚN ĐẦU ĐUÔI, LON DAU, LON DUOI...
     *  - NHỎ, NHỎ ĐẦU, NHỎ ĐUÔI, NHỎ ĐẦU ĐUÔI, NHO DAU, NHO DUOI...
     */
    private static boolean isLonNho(String cachDanh) {
        String u = normalizeCachDanh(cachDanh);
        // chỉ cần chứa "LON" hoặc "NHO" là kèo LỚN/NHỎ
        return u.contains("LON") || u.contains("NHO");
    }

    /**
     * Chuẩn hoá cách đánh:
     *  - Bỏ dấu tiếng Việt
     *  - In hoa
     *  - Bỏ ký tự không phải chữ/số/space
     *  - Gom khoảng trắng
     *
     * Ví dụ:
     *  "LỚN ĐUÔI"      -> "LON DUOI"
     *  "nhỏ đầu đuôi"  -> "NHO DAU DUOI"
     *  "LON-DAU"       -> "LON DAU"
     */
    private static String normalizeCachDanh(String s) {
        if (s == null) return "";
        // Bỏ dấu
        String noDia = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        // In hoa + trim
        String u = noDia.toUpperCase().trim();
        // Chỉ giữ chữ/số/space, những ký tự khác (.,-/…) thay bằng space
        u = u.replaceAll("[^A-Z0-9\\s]", " ");
        // Gom nhiều space về 1
        u = u.replaceAll("\\s+", " ");
        return u;
    }

    /** Map các biến thể tên miền → mã miền ổn định */
    private static String toMienCode(String raw) {
        String s = safeUpper(raw);
        if (s.startsWith("MB") || s.contains("BẮC") || s.contains("BAC")) return "MB";
        if (s.startsWith("MT") || s.contains("TRUNG"))                  return "MT";
        if (s.startsWith("MN") || s.contains("NAM"))                    return "MN";
        return "??";
    }
}
