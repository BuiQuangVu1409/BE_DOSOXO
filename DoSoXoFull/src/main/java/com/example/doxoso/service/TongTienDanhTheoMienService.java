//package com.example.doxoso.service;
//
//import com.example.doxoso.model.Bet;
//import com.example.doxoso.model.PlayerTongTienDanhTheoMienDto;
//import com.example.doxoso.repository.BetRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.text.Normalizer;
//import java.time.LocalDate;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class TongTienDanhTheoMienService {
//
//    private final BetRepository betRepository;
//
//    // Loại trừ MỌI cách đánh có chứa "LON" / "NHO" (LỚN / NHỎ, LỚN ĐẦU, LỚN ĐUÔI, LỚN ĐẦU ĐUÔI,...)
//    // Nếu sau này muốn tính luôn LỚN/NHỎ vào tổng tiền đánh thì sửa/ bỏ list này.
//    private static final List<String> EXCLUDED_KEYWORDS = List.of("LON", "NHO");
//
//    /** ------------ TÍNH CHO 1 PLAYER (TRẢ VỀ DTO) ------------ */
//    public PlayerTongTienDanhTheoMienDto tinhTongTheoMien(Long playerId) {
//        List<Bet> ds = betRepository.findByPlayer_Id(playerId);
//        CalcResult calc = tinhTuDanhSachSo(ds);
//
//        return PlayerTongTienDanhTheoMienDto.builder()
//                .playerId(playerId)
//                .playerName(calc.playerName)
//                .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
//                .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
//                .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
//                .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
//                .build();
//    }
//
//    /** ------------ TÍNH CHO TẤT CẢ PLAYER (LIST DTO) ------------ */
//    public List<PlayerTongTienDanhTheoMienDto> tinhTatCaPlayer() {
//        List<Bet> all = betRepository.findAllWithPlayer();
//
//        Map<Long, List<Bet>> byPlayer = all.stream()
//                .filter(s -> s.getPlayer() != null && s.getPlayer().getId() != null)
//                .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));
//
//        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();
//
//        for (Map.Entry<Long, List<Bet>> e : byPlayer.entrySet()) {
//            Long pid = e.getKey();
//            CalcResult calc = tinhTuDanhSachSo(e.getValue());
//
//            result.add(PlayerTongTienDanhTheoMienDto.builder()
//                    .playerId(pid)
//                    .playerName(calc.playerName)
//                    .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
//                    .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
//                    .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
//                    .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
//                    .build());
//        }
//
//        result.sort(Comparator.comparing(PlayerTongTienDanhTheoMienDto::getTong).reversed());
//        return result;
//    }
//
//    /** Tính từ 1 danh sách Bet của cùng player */
//    private CalcResult tinhTuDanhSachSo(List<Bet> danhSach) {
//        Map<String, BigDecimal> byRegion = new LinkedHashMap<>();
//        byRegion.put("MIEN BAC", BigDecimal.ZERO);
//        byRegion.put("MIEN TRUNG", BigDecimal.ZERO);
//        byRegion.put("MIEN NAM", BigDecimal.ZERO);
//
//        BigDecimal grandTotal = BigDecimal.ZERO;
//        String playerName = null;
//
//        for (Bet so : danhSach) {
//            // Lấy tên player nếu có
//            if (playerName == null && so.getPlayer() != null) {
//                try {
//                    Object n = so.getPlayer().getClass().getMethod("getName").invoke(so.getPlayer());
//                    if (n != null) playerName = String.valueOf(n);
//                } catch (ReflectiveOperationException ignored) {}
//            }
//
//            String cachDanhNorm = norm(so.getCachDanh());
//
//            // ❌ BỎ mọi cách đánh có chứa "LON" hoặc "NHO" (LỚN / NHỎ / LỚN ĐẦU / LỚN ĐUÔI / LỚN ĐẦU ĐUÔI...)
//            boolean isExcluded = EXCLUDED_KEYWORDS.stream().anyMatch(cachDanhNorm::contains);
//            if (isExcluded) continue;
//
//            String mien = normMien(so.getMien());
//
//            // 1) Lấy tiền trên 1 dòng BET
//            BigDecimal tien = parseTienDanh(so.getSoTien());
//
//            // 2) Nhân theo số đài thực tế (2 đài / 3 đài)
//            long soDai = countSoDai(so);
//            if (soDai > 1) {
//                tien = tien.multiply(BigDecimal.valueOf(soDai));
//            }
//
//            // 3) Cộng vào map theo miền + tổng
//            if (byRegion.containsKey(mien)) {
//                byRegion.put(mien, byRegion.get(mien).add(tien));
//            } else {
//                byRegion.put(mien, tien);
//            }
//            grandTotal = grandTotal.add(tien);
//        }
//
//        byRegion.put("TONG", grandTotal);
//        return new CalcResult(playerName, byRegion);
//    }
//
//    /** 👉 Tính tổng tiền theo *từng ngày* cho 1 player trong khoảng [from, to] */
//    @Transactional
//    public List<PlayerTongTienDanhTheoMienDto> tinhTongTheoMienTheoNgay(Long playerId, LocalDate from, LocalDate to) {
//        List<Bet> all = betRepository.findByPlayer_IdAndNgayBetween(playerId, from, to);
//
//        Map<LocalDate, List<Bet>> byDate = all.stream()
//                .collect(Collectors.groupingBy(Bet::getNgay, TreeMap::new, Collectors.toList()));
//
//        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();
//
//        for (Map.Entry<LocalDate, List<Bet>> e : byDate.entrySet()) {
//            LocalDate ngay = e.getKey();
//            var calc = tinhTuDanhSachSo(e.getValue());
//
//            result.add(PlayerTongTienDanhTheoMienDto.builder()
//                    .playerId(playerId)
//                    .playerName(calc.playerName)
//                    .ngay(ngay)
//                    .mienBac(calc.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
//                    .mienTrung(calc.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
//                    .mienNam(calc.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
//                    .tong(calc.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
//                    .build());
//        }
//        return result;
//    }
//
//    // ===== Helpers =====
//
////    private static String norm(String s) {
////        if (s == null) return "";
////        String noDia = Normalizer.normalize(s, Normalizer.Form.NFD)
////                .replaceAll("\\p{M}+", "");
////        String upper = noDia.toUpperCase(Locale.ROOT).trim();
////        return upper.replaceAll("\\s+", " ");
////    }
//private static String norm(String s) {
//    if (s == null) return "";
//    // 1. Xử lý chữ Đ/đ riêng trước khi chuẩn hóa
//    String temp = s.replace("đ", "d").replace("Đ", "D");
//
//    // 2. Chuẩn hóa NFD và loại bỏ dấu thanh
//    String noDia = Normalizer.normalize(temp, Normalizer.Form.NFD)
//            .replaceAll("\\p{M}+", "");
//
//    // 3. Trim và Upper
//    String upper = noDia.toUpperCase(Locale.ROOT).trim();
//    return upper.replaceAll("\\s+", " ");
//}
//    private static String normMien(String s) {
//        String n = norm(s);
//        if (n.isEmpty()) return "";
//
//        if (n.equals("MB")) return "MIEN BAC";
//        if (n.equals("MT")) return "MIEN TRUNG";
//        if (n.equals("MN")) return "MIEN NAM";
//
//        if (n.contains("BAC")) return "MIEN BAC";
//        if (n.contains("TRUNG")) return "MIEN TRUNG";
//        if (n.contains("NAM")) return "MIEN NAM";
//
//        return n;
//    }
//
//    private static BigDecimal parseTienDanh(String s) {
//        if (s == null || s.isBlank()) return BigDecimal.ZERO;
//
//        String cleaned = s.replace(".", "").replace(",", "").trim();
//
//        try {
//            if (cleaned.contains("-")) {
//                BigDecimal sum = BigDecimal.ZERO;
//                for (String part : cleaned.split("-")) {
//                    String p = part.trim();
//                    if (!p.isBlank()) {
//                        sum = sum.add(new BigDecimal(p));
//                    }
//                }
//                return sum;
//            }
//            return new BigDecimal(cleaned);
//        } catch (NumberFormatException ex) {
//            return BigDecimal.ZERO;
//        }
//    }
//
//    /**
//     * Đếm số đài từ field "dai" / "tenDai" của Bet.
//     * - "3 đài"  → 3
//     * - "2 đài"  → 2
//     * - "Cà Mau" → 1
//     * - "Cà Mau, TP.HCM, Đồng Tháp" → 3
//     */
//    private static long countSoDai(Bet so) {
//        List<String> candidates = new ArrayList<>();
//
//        // 1. Ưu tiên lấy trực tiếp (Nếu Bet có getDai() thì hãy dùng so.getDai())
//        // Giả sử dùng Reflection như cũ nhưng thêm log
//        try {
//            // Kiểm tra các trường có thể
//            addIfNotNull(candidates, so, "getDai");
//            addIfNotNull(candidates, so, "getTenDai");
//            addIfNotNull(candidates, so, "getLoaiDai");
//        } catch (Exception e) {
//            // e.printStackTrace(); // Bật lên nếu cần debug
//        }
//
//        // Ưu tiên số đài cứng (nếu có)
//        try {
//            Object val = so.getClass().getMethod("getSoDai").invoke(so);
//            if (val != null) return Long.parseLong(val.toString());
//        } catch (Exception ignored) {}
//
//        for (String raw : candidates) {
//            String n = norm(raw); // Lúc này "2 đài" đã thành "2 DAI" chuẩn
//
//            // Debug: In ra để xem nó đọc được gì (Xóa sau khi fix xong)
//            // System.out.println("Check so dai: Raw='" + raw + "' -> Norm='" + n + "'");
//
//            if (n.contains("3 DAI")) return 3L;
//            if (n.contains("2 DAI")) return 2L;
//            if (n.contains("4 DAI")) return 4L;
//
//            // Xử lý dấu phẩy: "TP.HCM, Cà Mau"
//            if (raw.contains(",")) {
//                return Arrays.stream(raw.split(","))
//                        .map(String::trim)
//                        .filter(str -> !str.isEmpty())
//                        .count();
//            }
//        }
//        return 1L;
//    }
//
//    // Helper nhỏ để code gọn hơn
//    private static void addIfNotNull(List<String> list, Bet so, String methodName) {
//        try {
//            Object v = so.getClass().getMethod(methodName).invoke(so);
//            if (v != null) list.add(v.toString());
//        } catch (Exception ignored) {}
//    }
//
//
////    private static long countSoDai(Bet so) {
////        // TODO: nếu entity dùng getDai() thì đổi lại ở đây
////        String raw = null;
////        try {
////            raw = (String) so.getClass().getMethod("getTenDai").invoke(so);
////        } catch (Exception ignored) {
////        }
////
////        if (raw == null || raw.isBlank()) return 1L;
////
////        // Nếu lưu dạng list "Cà Mau, TP.HCM, Đồng Tháp"
////        if (raw.contains(",")) {
////            return Arrays.stream(raw.split(","))
////                    .map(String::trim)
////                    .filter(s -> !s.isEmpty())
////                    .count();
////        }
////
////        // Nếu lưu dạng "3 đài", "2 dai"
////        String n = norm(raw); // => "3 DAI", "2 DAI", "HA NOI"
////        if (n.contains("3 DAI")) return 3L;
////        if (n.contains("2 DAI")) return 2L;
////
////        // Mặc định 1 đài
////        return 1L;
////    }
//
//    private record CalcResult(String playerName, Map<String, BigDecimal> mapTheoMien) {}
//}
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

    // Các từ khóa loại trừ (Lớn/Nhỏ/Chẵn/Lẻ...)
    private static final List<String> EXCLUDED_KEYWORDS = List.of("LON", "NHO");

    /** ------------ TÍNH CHO 1 PLAYER (TRẢ VỀ DTO) ------------ */
    public PlayerTongTienDanhTheoMienDto tinhTongTheoMien(Long playerId) {
        List<Bet> ds = betRepository.findByPlayer_Id(playerId);
        CalcResult calc = tinhTuDanhSachSo(ds);
        return buildDto(playerId, calc, null);
    }

    /** ------------ TÍNH CHO TẤT CẢ PLAYER (LIST DTO) ------------ */
    public List<PlayerTongTienDanhTheoMienDto> tinhTatCaPlayer() {
        List<Bet> all = betRepository.findAllWithPlayer();
        Map<Long, List<Bet>> byPlayer = all.stream()
                .filter(s -> s.getPlayer() != null && s.getPlayer().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));

        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();
        for (Map.Entry<Long, List<Bet>> e : byPlayer.entrySet()) {
            result.add(buildDto(e.getKey(), tinhTuDanhSachSo(e.getValue()), null));
        }
        result.sort(Comparator.comparing(PlayerTongTienDanhTheoMienDto::getTong).reversed());
        return result;
    }

    /** 👉 Tính tổng tiền theo *từng ngày* cho 1 player */
    @Transactional
    public List<PlayerTongTienDanhTheoMienDto> tinhTongTheoMienTheoNgay(Long playerId, LocalDate from, LocalDate to) {
        List<Bet> all = betRepository.findByPlayer_IdAndNgayBetween(playerId, from, to);
        Map<LocalDate, List<Bet>> byDate = all.stream()
                .collect(Collectors.groupingBy(Bet::getNgay, TreeMap::new, Collectors.toList()));

        List<PlayerTongTienDanhTheoMienDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Bet>> e : byDate.entrySet()) {
            result.add(buildDto(playerId, tinhTuDanhSachSo(e.getValue()), e.getKey()));
        }
        return result;
    }

    // =================================================================
    // ⚙️ LOGIC TÍNH TOÁN CHÍNH (CORE)
    // =================================================================
    private CalcResult tinhTuDanhSachSo(List<Bet> danhSach) {
        Map<String, BigDecimal> byRegion = new LinkedHashMap<>();
        byRegion.put("MIEN BAC", BigDecimal.ZERO);
        byRegion.put("MIEN TRUNG", BigDecimal.ZERO);
        byRegion.put("MIEN NAM", BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;
        String playerName = null;

        for (Bet so : danhSach) {
            // Lấy tên player (nếu chưa có)
            if (playerName == null && so.getPlayer() != null) {
                playerName = getPlayerNameSafe(so.getPlayer());
            }

            // 1️⃣ Chuẩn hóa Cách Đánh & Kiểm tra loại trừ
            String cachDanhNorm = norm(so.getCachDanh());
            if (EXCLUDED_KEYWORDS.stream().anyMatch(cachDanhNorm::contains)) {
                continue; // Bỏ qua Lớn/Nhỏ
            }

            // 2️⃣ Xác định Miền
            String mien = normMien(so.getMien());

            // 3️⃣ Lấy tiền gốc
            BigDecimal tien = parseTienDanh(so.getSoTien());

            // 4️⃣ NHÂN SỐ ĐÀI (Quan trọng cho MN/MT)
            long soDai = countSoDai(so);
            if (soDai > 1) {
                tien = tien.multiply(BigDecimal.valueOf(soDai));
            }

            // 5️⃣ Cộng dồn
            if (byRegion.containsKey(mien)) {
                byRegion.put(mien, byRegion.get(mien).add(tien));
            } else {
                byRegion.put(mien, tien); // Fallback nếu có miền lạ
            }
            grandTotal = grandTotal.add(tien);
        }

        byRegion.put("TONG", grandTotal);
        return new CalcResult(playerName, byRegion);
    }

    // =================================================================
    // 🛠️ CÁC HÀM HELPER (ĐÃ FIX LỖI "Đ" VÀ LOGIC ĐẾM ĐÀI)
    // =================================================================

    /**
     * Chuẩn hóa chuỗi:
     * - Chuyển "đ" -> "d" (Fix lỗi không nhận diện được "2 đài")
     * - Bỏ dấu tiếng Việt, UpperCase
     */
    private static String norm(String s) {
        if (s == null) return "";
        // Bước 1: Thay thế đ/Đ thủ công trước khi normalize NFD
        String temp = s.replace("đ", "d").replace("Đ", "D");

        // Bước 2: Chuẩn hóa Unicode
        String noDia = Normalizer.normalize(temp, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        // Bước 3: Trim và Upper
        return noDia.toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /**
     * Đếm số đài:
     * - Ưu tiên tìm "2 DAI", "3 DAI", "4 DAI" (Miền Nam hay có 4 đài)
     * - Đếm dấu phẩy
     * - Hỗ trợ cả getDai(), getTenDai()
     */
    private static long countSoDai(Bet so) {
        List<String> candidates = new ArrayList<>();

        // Lấy dữ liệu từ các trường có thể chứa thông tin đài
        addValIfNotNull(candidates, so, "getDai");      // Ưu tiên 1
        addValIfNotNull(candidates, so, "getTenDai");   // Ưu tiên 2
        addValIfNotNull(candidates, so, "getLoaiDai");  // Ưu tiên 3

        // Nếu có trường số đài cứng (int/long)
        try {
            Object v = so.getClass().getMethod("getSoDai").invoke(so);
            if (v != null) return Long.parseLong(v.toString());
        } catch (Exception ignored) {}

        for (String raw : candidates) {
            String n = norm(raw); // Đã xử lý 'đ' -> 'd'

            // Check keywords (Miền Nam thường có 3 đài, 4 đài)
            if (n.contains("4 DAI")) return 4L;
            if (n.contains("3 DAI")) return 3L;
            if (n.contains("2 DAI")) return 2L;

            // Đếm dấu phẩy (VD: "TP.HCM, Long An, Bình Phước")
            if (raw.contains(",")) {
                long count = Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .count();
                if (count > 0) return count;
            }
        }
        return 1L; // Mặc định 1 đài (cho Miền Bắc hoặc không xác định)
    }

    private static void addValIfNotNull(List<String> list, Object obj, String methodName) {
        try {
            Object v = obj.getClass().getMethod(methodName).invoke(obj);
            if (v != null) list.add(v.toString());
        } catch (Exception ignored) {}
    }

    private static String normMien(String s) {
        String n = norm(s);
        if (n.isEmpty()) return "";
        if (n.equals("MB") || n.contains("BAC")) return "MIEN BAC";
        if (n.equals("MT") || n.contains("TRUNG")) return "MIEN TRUNG";
        if (n.equals("MN") || n.contains("NAM")) return "MIEN NAM";
        return n;
    }

    private static BigDecimal parseTienDanh(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;

        // ❌ CŨ (SAI): String cleaned = s.replace(".", "").replace(",", "").trim();

        // ✅ MỚI (ĐÚNG): Chỉ xóa dấu phẩy (nếu coi nó là ngăn cách hàng ngàn)
        // Giữ nguyên dấu chấm để Java hiểu là số thập phân (24.5)
        String cleaned = s.replace(",", "").trim();

        try {
            if (cleaned.contains("-")) {
                BigDecimal sum = BigDecimal.ZERO;
                String[] parts = cleaned.split("-");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.isEmpty()) continue;
                    sum = sum.add(new BigDecimal(p));
                }
                return sum;
            }
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String getPlayerNameSafe(Object player) {
        try {
            Object n = player.getClass().getMethod("getName").invoke(player);
            return n != null ? String.valueOf(n) : null;
        } catch (Exception e) { return null; }
    }

    private PlayerTongTienDanhTheoMienDto buildDto(Long pid, CalcResult c, LocalDate date) {
        return PlayerTongTienDanhTheoMienDto.builder()
                .playerId(pid)
                .playerName(c.playerName)
                .ngay(date)
                .mienBac(c.mapTheoMien.getOrDefault("MIEN BAC", BigDecimal.ZERO))
                .mienTrung(c.mapTheoMien.getOrDefault("MIEN TRUNG", BigDecimal.ZERO))
                .mienNam(c.mapTheoMien.getOrDefault("MIEN NAM", BigDecimal.ZERO))
                .tong(c.mapTheoMien.getOrDefault("TONG", BigDecimal.ZERO))
                .build();
    }

    private record CalcResult(String playerName, Map<String, BigDecimal> mapTheoMien) {}
}