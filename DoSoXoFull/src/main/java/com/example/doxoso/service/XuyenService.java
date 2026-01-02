//package com.example.doxoso.service;
//
//import com.example.doxoso.model.Bet;
//import com.example.doxoso.model.DoiChieuKetQuaDto;
//import com.example.doxoso.repository.KetQuaMienBacRepository;
//import com.example.doxoso.repository.KetQuaMienNamRepository;
//import com.example.doxoso.repository.KetQuaMienTrungRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class XuyenService {
//
//    @Autowired
//    private KetQuaMienBacRepository bacRepo;
//
//    @Autowired
//    private KetQuaMienTrungRepository trungRepo;
//
//    @Autowired
//    private KetQuaMienNamRepository namRepo;
//
//    @Autowired
//    private TinhTienService tinhTienService;
//
//    // ===================== PUBLIC APIs =====================
//
//    /** [unchanged] Nhận diện có phải cách đánh XUYÊN không */
//    public boolean laCachDanhXuyen(String cachDanh) {
//        String cd = chuanHoaCachDanhXuyen(cachDanh);
//        return cd != null && cd.startsWith("XUYEN");
//    }
//
//    /** [UPDATED] Dò Xuyên per-đài (tiền đánh là PER-ĐÀI, tổng tiền trúng = cộng các đài TRÚNG) */
//    public DoiChieuKetQuaDto xuLyXuyen(Bet bet) {
//        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
//        dto.setSoDanh(bet.getSoDanh());
//        dto.setCachTrung("XUYÊN");
//        dto.setCachDanh(bet.getCachDanh());
//        dto.setNgay(bet.getNgay());
//        dto.setMien(bet.getMien());
//        dto.setTenDai(bet.getDai());
//
//        // -------- 0) Validate cơ bản --------
//        // [NEW] Chuẩn hóa & kiểm tra cách đánh XUYÊN{2,3,4,5}
//        final String cachDanhNorm = chuanHoaCachDanhXuyen(bet.getCachDanh());
//        if (cachDanhNorm == null || !cachDanhNorm.matches("XUYEN[2-5]")) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Cách đánh không hợp lệ (chỉ hỗ trợ XUYÊN2..XUYÊN5)."));
//            dto.setKetQuaTungDai(List.of());
//            return dto;
//        }
//        final int soXuyen = Integer.parseInt(cachDanhNorm.replace("XUYEN", ""));
//
//        // [NEW] Chuẩn hóa dãy số người chơi (2 chữ số, bỏ 0 thừa, ví dụ: "2" -> "02")
//        List<String> soNguoiChoi = parseSoNguoiChoi2ChuSo(bet.getSoDanh());
//        if (soNguoiChoi.size() < 2) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải từ 2 số, định dạng 2 chữ số, ví dụ '05-17')."));
//            dto.setKetQuaTungDai(List.of());
//            return dto;
//        }
//
//        // [NEW] Tiền XUYÊN là PER-ĐÀI (không chia cho số đài)
//        final double tienDanhPerDai;
//        try {
//            // [UPDATED] dùng bet.getTienDanh() thay vì getSoTien()
//            tienDanhPerDai = Double.parseDouble(bet.getSoTien());
//        } catch (NumberFormatException e) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ: " + bet.getSoTien()));
//            dto.setKetQuaTungDai(List.of());
//            return dto;
//        }
//
//        // -------- 1) Lấy danh sách đài cần dò (theo DB + yêu cầu người chơi) --------
////        final List<String> danhSachDai = xacDinhDanhSachDaiCanDo(bet); // [NEW]
////        dto.setDanhSachDai(new ArrayList<>(danhSachDai));
//        final List<String> danhSachDai = xacDinhDanhSachDaiCanDo(bet, dto); // ✅ truyền dto để báo lỗi
//        if (danhSachDai == null) return dto; // ✅ nếu lỗi (bỏ trống mà nhiều đài) thì dừng luôn
//        dto.setDanhSachDai(new ArrayList<>(danhSachDai));
//
//        // -------- 2) Kéo kết quả theo ngày + miền (an toàn với dấu) --------
//        LocalDate ngay = bet.getNgay();
//        String mienNorm = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();
//
//        List<Object> ketQuaTongHop = new ArrayList<>();
//        if (mienNorm.contains("BAC"))   ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
//        if (mienNorm.contains("TRUNG")) ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
//        if (mienNorm.contains("NAM"))   ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
//        if (ketQuaTongHop.isEmpty()) { // fallback nếu miền nhập không rõ
//            ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
//            ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
//            ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
//        }
//
//        // Gom kết quả theo ĐÀI, chỉ giữ những đài đã chọn
//        Map<String, List<Object>> ketQuaTheoDai = ketQuaTongHop.stream()
//                .filter(kq -> {
//                    Object ten = safeGet(kq, "getTenDai");
//                    return ten instanceof String && danhSachDai.contains(((String) ten).trim().toUpperCase());
//                })
//                .collect(Collectors.groupingBy(kq -> ((String) safeGet(kq, "getTenDai")).trim().toUpperCase()));
//
//        // -------- 3) Dò từng đài & tính tiền --------
//        List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTungDai = new ArrayList<>();
//        boolean coTrung = false;
//        double tongTien = 0.0;
//
//        for (String tenDai : danhSachDai) {
//            DoiChieuKetQuaDto.KetQuaTheoDai kqDai = new DoiChieuKetQuaDto.KetQuaTheoDai();
//            kqDai.setTenDai(tenDai);
//            // [NEW] xác định miền đúng cho đài này (MIỀN BẮC/TRUNG/NAM)
//            kqDai.setMien(xacDinhMienCuaDai(tenDai, ngay));
//
//            List<Object> ketQuaDai = ketQuaTheoDai.getOrDefault(tenDai, List.of());
//
//            // [NEW] Lấy list 2 số cuối của đài (distinct để đếm combo cho đúng)
//            Set<String> so2CuoiTrongDai = ketQuaDai.stream()
//                    .map(kq -> (String) safeGet(kq, "getSoTrung"))
//                    .filter(Objects::nonNull)
//                    .map(String::trim)
//                    .filter(s -> s.length() >= 2)
//                    .map(s -> s.substring(s.length() - 2))
//                    .map(this::pad2)
//                    .collect(Collectors.toCollection(LinkedHashSet::new));
//
//            // [NEW] Đếm số combo trúng:
//            // - Nếu số người chơi = soXuyen => 1 combo duy nhất
//            // - Nếu nhiều hơn => xét C(n, soXuyen) combos, combo nào có tất cả số ∈ so2CuoiTrongDai thì trúng
//            int soComboTrung = demComboTrung(soNguoiChoi, soXuyen, so2CuoiTrongDai);
//
//            if (soComboTrung > 0) {
//                kqDai.setTrung(true);
//                kqDai.setSoLanTrung(soComboTrung);
//                kqDai.setSoTrung(String.join(",", soNguoiChoi)); // show bộ số gốc
//                // [NEW] tính tiền PER-ĐÀI = heSo(mien, soXuyen) * tienDanhPerDai * soComboTrung
//                double tien1Lan = tinhTienService.tinhTienXuyen(cachDanhNorm, bet.getSoTien(), kqDai.getMien());
//                double tienDai = tien1Lan * soComboTrung;
//                kqDai.setTienTrung(tienDai);
//
//                tongTien += tienDai;
//                coTrung = true;
//            } else {
//                kqDai.setTrung(false);
//                kqDai.setSoLanTrung(0);
//                // ghi rõ thiếu số nào (soNguoiChoi - so2CuoiTrongDai)
//                List<String> thieu = soNguoiChoi.stream()
//                        .filter(s -> !so2CuoiTrongDai.contains(s))
//                        .collect(Collectors.toList());
//                kqDai.setLyDo("Thiếu số: " + String.join(",", thieu));
//                kqDai.setTienTrung(0.0);
//            }
//
//            ketQuaTungDai.add(kqDai);
//        }
//
//        dto.setTrung(coTrung);
//        dto.setTienTrung(tongTien);
//        dto.setKetQuaTungDai(ketQuaTungDai);
//        return dto;
//    }
//
//    // ===================== HELPERS =====================
//
////    /** [NEW] Lấy danh sách ĐÀI cần dò từ DB + tôn trọng bet.getDai(): "N ĐÀI" hoặc danh sách tên */
////    private List<String> xacDinhDanhSachDaiCanDo(Bet bet) {
////        LocalDate ngay = bet.getNgay();
////        String mienNorm = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();
////
////        // Lấy tất cả đài mở thưởng thực tế theo ngày + miền
////        List<Object> ketQuaNgay = new ArrayList<>();
////        if (mienNorm.contains("BAC"))   ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
////        if (mienNorm.contains("TRUNG")) ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
////        if (mienNorm.contains("NAM"))   ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
////        if (ketQuaNgay.isEmpty()) { // fallback khi miền không rõ
////            ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
////            ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
////            ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
////        }
////
////        List<String> dsFromDB = ketQuaNgay.stream()
////                .map(kq -> (String) safeGet(kq, "getTenDai"))
////                .filter(Objects::nonNull)
////                .map(String::trim)
////                .filter(s -> !s.isEmpty())
////                .map(String::toUpperCase)
////                .distinct()
////                .collect(Collectors.toList());
////
////        String daiInput = bet.getDai();
////        if (daiInput == null || daiInput.isBlank()) return dsFromDB;
////
////        String upper = daiInput.trim().toUpperCase();
////        String noDia = removeDiacritics(upper);
////
////        // "N DAI" / "N ĐÀI"
////        if (noDia.matches("\\d+\\s*DAI")) {
////            int n = Integer.parseInt(noDia.replaceAll("\\D+", ""));
////            if (n <= 0) n = 1;
////            if (dsFromDB.size() <= n) return dsFromDB;
////            return dsFromDB.subList(0, n);
////        }
////
////        // Tên đài phân tách dấu phẩy
////        List<String> dsUser = Arrays.stream(upper.split(","))
////                .map(String::trim).filter(s -> !s.isEmpty())
////                .map(String::toUpperCase).distinct().collect(Collectors.toList());
////        List<String> giao = dsUser.stream().filter(dsFromDB::contains).collect(Collectors.toList());
////        return giao.isEmpty() ? dsFromDB : giao;
////    }
//    /** [UPDATED] Lấy danh sách ĐÀI cần dò từ DB + tôn trọng bet.getDai()
//     *  - Nếu bet.getDai() rỗng:
//     *      + Nếu chỉ có 1 đài trong ngày -> cho phép
//     *      + Nếu >1 đài -> BÁO LỖI (PHƯƠNG ÁN A)
//     */
//    private List<String> xacDinhDanhSachDaiCanDo(Bet bet, DoiChieuKetQuaDto dto) {
//        LocalDate ngay = bet.getNgay();
//        String mienNorm = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();
//
//        // Lấy tất cả đài mở thưởng thực tế theo ngày + miền
//        List<Object> ketQuaNgay = new ArrayList<>();
//        if (mienNorm.contains("BAC"))   ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
//        if (mienNorm.contains("TRUNG")) ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
//        if (mienNorm.contains("NAM"))   ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
//        if (ketQuaNgay.isEmpty()) { // fallback khi miền không rõ
//            ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
//            ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
//            ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
//        }
//
//        List<String> dsFromDB = ketQuaNgay.stream()
//                .map(kq -> (String) safeGet(kq, "getTenDai"))
//                .filter(Objects::nonNull)
//                .map(String::trim)
//                .filter(s -> !s.isEmpty())
//                .map(String::toUpperCase)
//                .distinct()
//                .collect(Collectors.toList());
//
//        String daiInput = bet.getDai();
//
//        // ✅ PHƯƠNG ÁN A:
//        // - rỗng + chỉ 1 đài => ok
//        // - rỗng + nhiều đài => báo lỗi
//        if (daiInput == null || daiInput.isBlank()) {
//            if (dsFromDB.size() == 1) return dsFromDB;
//
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Bạn phải nhập tên đài muốn dò, hoặc “N ĐÀI” khớp số đài hôm nay."));
//            dto.setDanhSachDai(dsFromDB);      // trả list đài thực tế để người chơi biết
//            dto.setKetQuaTungDai(List.of());   // không dò nữa
//            return null;
//        }
//
//        String upper = daiInput.trim().toUpperCase();
//        String noDia = removeDiacritics(upper);
//
//        // "N DAI" / "N ĐÀI"
//        if (noDia.matches("\\d+\\s*DAI")) {
//            int n = Integer.parseInt(noDia.replaceAll("\\D+", ""));
//            if (n <= 0) n = 1;
//            if (dsFromDB.size() <= n) return dsFromDB;
//            return dsFromDB.subList(0, n);
//        }
//
//        // Tên đài phân tách dấu phẩy
//        List<String> dsUser = Arrays.stream(upper.split(","))
//                .map(String::trim).filter(s -> !s.isEmpty())
//                .map(String::toUpperCase).distinct().collect(Collectors.toList());
//
//        List<String> giao = dsUser.stream().filter(dsFromDB::contains).collect(Collectors.toList());
//        return giao.isEmpty() ? dsFromDB : giao;
//    }
//
//    /** [NEW] đếm số combo trúng: nếu số người chơi > soXuyen, xét tất cả C(n, soXuyen) */
//    private int demComboTrung(List<String> soNguoiChoi, int soXuyen, Set<String> so2CuoiTrongDai) {
//        if (soNguoiChoi.size() < soXuyen) return 0;
//        int[] count = {0};
//        toHop(soNguoiChoi, soXuyen, 0, new ArrayDeque<>(), combo -> {
//            boolean ok = combo.stream().allMatch(so2CuoiTrongDai::contains);
//            if (ok) count[0]++;
//        });
//        return count[0];
//    }
//
//    /** [NEW] sinh tổ hợp size k từ list (callback từng combo) */
//    private <T> void toHop(List<T> arr, int k, int idx, Deque<T> path, java.util.function.Consumer<List<T>> visit) {
//        if (path.size() == k) { visit.accept(List.copyOf(path)); return; }
//        for (int i = idx; i <= arr.size() - (k - path.size()); i++) {
//            path.addLast(arr.get(i));
//            toHop(arr, k, i + 1, path, visit);
//            path.removeLast();
//        }
//    }
//
//    /** [NEW] chuẩn hóa mảng số người chơi thành 2 chữ số (pad-left '0') */
//    private List<String> parseSoNguoiChoi2ChuSo(String soDanhRaw) {
//        if (soDanhRaw == null) return List.of();
//        return Arrays.stream(soDanhRaw.split("[-,]"))
//                .map(String::trim)
//                .filter(s -> !s.isEmpty())
//                .map(this::stripLeadingZeros)
//                .map(this::pad2)
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    private String stripLeadingZeros(String s) {
//        String t = s.replaceFirst("^0+(?!$)", "");
//        return t.isEmpty() ? "0" : t;
//    }
//    private String pad2(String s) {
//        return (s.length() == 1) ? "0" + s : (s.length() >= 2 ? s.substring(s.length() - 2) : s);
//    }
//
//    /** [UPDATED] robust contains BẮC/TRUNG/NAM theo DB thực tế */
//    private String xacDinhMienCuaDai(String tenDai, LocalDate ngay) {
//        if (bacRepo.findAllByNgay(ngay).stream()
//                .anyMatch(kq -> tenDai.equalsIgnoreCase((String) safeGet(kq, "getTenDai")))) {
//            return "MIỀN BẮC";
//        }
//        if (trungRepo.findAllByNgay(ngay).stream()
//                .anyMatch(kq -> tenDai.equalsIgnoreCase((String) safeGet(kq, "getTenDai")))) {
//            return "MIỀN TRUNG";
//        }
//        if (namRepo.findAllByNgay(ngay).stream()
//                .anyMatch(kq -> tenDai.equalsIgnoreCase((String) safeGet(kq, "getTenDai")))) {
//            return "MIỀN NAM";
//        }
//        return "KHÔNG RÕ";
//    }
//
//    // ===================== Utils =====================
//
//    @SuppressWarnings("unchecked")
//    private Object safeGet(Object obj, String method) {
//        try { return obj.getClass().getMethod(method).invoke(obj); }
//        catch (Exception e) { return null; }
//    }
//
//    public String chuanHoaCachDanhXuyen(String cachDanh) {
//        if (cachDanh == null) return null;
//        String cd = removeDiacritics(cachDanh).toUpperCase().replaceAll("[\\s\\.,]+", "");
//        cd = cd.replace("XIEN", "XUYEN");
//        if (cd.matches("\\d+XUYEN\\d*")) {
//            String so = cd.replaceAll("\\D+", "");
//            return "XUYEN" + so;
//        }
//        if (cd.matches("XUYEN\\d+")) return cd;
//        if (cd.equals("XUYEN")) return "XUYEN2";
//        return cd;
//    }
//
//    private String removeDiacritics(String input) {
//        String normalized = java.text.Normalizer.normalize(input == null ? "" : input, java.text.Normalizer.Form.NFD);
//        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
//                .matcher(normalized).replaceAll("");
//    }
//}
package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class XuyenService {

    @Autowired private KetQuaMienBacRepository bacRepo;
    @Autowired private KetQuaMienTrungRepository trungRepo;
    @Autowired private KetQuaMienNamRepository namRepo;
    @Autowired private TinhTienService tinhTienService;

    /** Nhận diện có phải cách đánh XUYÊN không */
    public boolean laCachDanhXuyen(String cachDanh) {
        String cd = chuanHoaCachDanhXuyen(cachDanh);
        return cd != null && cd.startsWith("XUYEN");
    }

    /** Dò Xuyên per-đài (tiền đánh là PER-ĐÀI, tổng tiền trúng = cộng các đài TRÚNG) */
    public DoiChieuKetQuaDto xuLyXuyen(Bet bet) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(bet.getSoDanh());
        dto.setCachTrung("XUYÊN");
        dto.setCachDanh(bet.getCachDanh());
        dto.setNgay(bet.getNgay());
        dto.setMien(bet.getMien());
        dto.setTenDai(bet.getDai());

        // -------- 0) Validate cơ bản --------
        final String cachDanhNorm = chuanHoaCachDanhXuyen(bet.getCachDanh());
        if (cachDanhNorm == null || !cachDanhNorm.matches("XUYEN[2-5]")) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Cách đánh không hợp lệ (chỉ hỗ trợ XUYÊN2..XUYÊN5)."));
            dto.setKetQuaTungDai(List.of());
            return dto;
        }
        final int soXuyen = Integer.parseInt(cachDanhNorm.replace("XUYEN", ""));

        // Chuẩn hóa dãy số người chơi (2 chữ số)
        List<String> soNguoiChoi = parseSoNguoiChoi2ChuSo(bet.getSoDanh());

        // ✅ FIX: XUYÊN3 phải >=3 số, XUYÊN4 >=4 số...
        if (soNguoiChoi.size() < soXuyen) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (XUYÊN" + soXuyen + " cần ít nhất " + soXuyen + " số)."));
            dto.setKetQuaTungDai(List.of());
            return dto;
        }

        // Validate tiền đánh (per-đài)
        final double tienDanhPerDai;
        try {
            tienDanhPerDai = Double.parseDouble(bet.getSoTien());
        } catch (NumberFormatException e) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ: " + bet.getSoTien()));
            dto.setKetQuaTungDai(List.of());
            return dto;
        }

        // -------- 1) Lấy danh sách đài cần dò (theo DB + yêu cầu người chơi) --------
        final List<String> danhSachDai = xacDinhDanhSachDaiCanDo(bet, dto);
        if (danhSachDai == null) return dto;
        dto.setDanhSachDai(new ArrayList<>(danhSachDai));

        // -------- 2) Kéo kết quả theo ngày + miền --------
        LocalDate ngay = bet.getNgay();
        String mienNorm = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();

        List<Object> ketQuaTongHop = new ArrayList<>();
        if (mienNorm.contains("BAC"))   ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
        if (mienNorm.contains("TRUNG")) ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
        if (mienNorm.contains("NAM"))   ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
        if (ketQuaTongHop.isEmpty()) {
            ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
            ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
            ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
        }

        // ✅ So sánh đài theo KEY không dấu để tránh lệch do dấu
        Set<String> daiKeySelected = danhSachDai.stream().map(this::daiKey).collect(Collectors.toSet());

        Map<String, List<Object>> ketQuaTheoDaiKey = ketQuaTongHop.stream()
                .filter(kq -> {
                    Object ten = safeGet(kq, "getTenDai");
                    if (!(ten instanceof String)) return false;
                    String key = daiKey(((String) ten).trim().toUpperCase());
                    return daiKeySelected.contains(key);
                })
                .collect(Collectors.groupingBy(kq -> {
                    String ten = (String) safeGet(kq, "getTenDai");
                    return daiKey(ten == null ? "" : ten.trim().toUpperCase());
                }));

        // -------- 3) Dò từng đài & tính tiền --------
        List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTungDai = new ArrayList<>();
        boolean coTrung = false;
        double tongTien = 0.0;

        for (String tenDaiDisplay : danhSachDai) {
            String keyDai = daiKey(tenDaiDisplay);

            DoiChieuKetQuaDto.KetQuaTheoDai kqDai = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kqDai.setTenDai(tenDaiDisplay);
            kqDai.setMien(xacDinhMienCuaDai(tenDaiDisplay, ngay));

            List<Object> ketQuaDai = ketQuaTheoDaiKey.getOrDefault(keyDai, List.of());

            // Lấy list 2 số cuối của đài (distinct)
            Set<String> so2CuoiTrongDai = ketQuaDai.stream()
                    .map(kq -> (String) safeGet(kq, "getSoTrung"))
                    .filter(Objects::nonNull)
                    .map(this::onlyDigits)
                    .filter(s -> s.length() >= 2)
                    .map(s -> s.substring(s.length() - 2))
                    .map(this::pad2)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            int soComboTrung = demComboTrung(soNguoiChoi, soXuyen, so2CuoiTrongDai);

            if (soComboTrung > 0) {
                kqDai.setTrung(true);
                kqDai.setSoLanTrung(soComboTrung);
                kqDai.setSoTrung(String.join(", ", soNguoiChoi));

                // tiền 1 lần trúng (per-đài)
                double tien1Lan = tinhTienService.tinhTienXuyen(cachDanhNorm, String.valueOf(tienDanhPerDai), kqDai.getMien());
                double tienDai = tien1Lan * soComboTrung;

                kqDai.setTienTrung(tienDai);
                tongTien += tienDai;
                coTrung = true;
            } else {
                kqDai.setTrung(false);
                kqDai.setSoLanTrung(0);
                List<String> thieu = soNguoiChoi.stream()
                        .filter(s -> !so2CuoiTrongDai.contains(s))
                        .collect(Collectors.toList());
                kqDai.setLyDo("Thiếu số: " + String.join(", ", thieu));
                kqDai.setTienTrung(0.0);
            }

            ketQuaTungDai.add(kqDai);
        }

        dto.setTrung(coTrung);
        dto.setTienTrung(tongTien);
        dto.setKetQuaTungDai(ketQuaTungDai);
        return dto;
    }

    // ===================== HELPERS =====================

    /**
     * Lấy danh sách ĐÀI cần dò từ DB + tôn trọng bet.getDai()
     *  - Nếu bet.getDai() rỗng:
     *      + Nếu chỉ có 1 đài trong ngày -> cho phép
     *      + Nếu >1 đài -> BÁO LỖI (PHƯƠNG ÁN A)
     */
    private List<String> xacDinhDanhSachDaiCanDo(Bet bet, DoiChieuKetQuaDto dto) {
        LocalDate ngay = bet.getNgay();
        String mienNorm = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();

        List<Object> ketQuaNgay = new ArrayList<>();
        if (mienNorm.contains("BAC"))   ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
        if (mienNorm.contains("TRUNG")) ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
        if (mienNorm.contains("NAM"))   ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
        if (ketQuaNgay.isEmpty()) {
            ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
            ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
            ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
        }

        // ✅ build map KEY->DISPLAY để so khớp không dấu nhưng vẫn trả đúng tên đài
        LinkedHashMap<String, String> mapKeyToDisplay = new LinkedHashMap<>();
        for (Object kq : ketQuaNgay) {
            String ten = (String) safeGet(kq, "getTenDai");
            if (ten == null) continue;
            String display = ten.trim().toUpperCase();
            if (display.isEmpty()) continue;
            mapKeyToDisplay.putIfAbsent(daiKey(display), display);
        }

        List<String> dsFromDB = new ArrayList<>(mapKeyToDisplay.values());

        String daiInput = bet.getDai();

        // ✅ PHƯƠNG ÁN A
        if (daiInput == null || daiInput.isBlank()) {
            if (dsFromDB.size() == 1) return dsFromDB;

            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Bạn phải nhập tên đài muốn dò, hoặc “N ĐÀI” khớp số đài hôm nay."));
            dto.setDanhSachDai(dsFromDB);
            dto.setKetQuaTungDai(List.of());
            return null;
        }

        String upper = daiInput.trim().toUpperCase();
        String noDia = removeDiacritics(upper);

        // ✅ "N DAI" / "N ĐÀI" (giờ đã nhận đúng vì removeDiacritics xử lý Đ/đ)
        if (noDia.matches("\\d+\\s*DAI")) {
            int n = Integer.parseInt(noDia.replaceAll("\\D+", ""));
            if (n <= 0) n = 1;

            // ✅ Khuyến nghị “khớp số đài hôm nay” (đúng ý bạn)
            if (n != dsFromDB.size()) {
                dto.setTrung(false);
                dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of("Số đài nhập (" + n + ") không khớp số đài mở thưởng hôm nay (" + dsFromDB.size() + ")."));
                dto.setDanhSachDai(dsFromDB);
                dto.setKetQuaTungDai(List.of());
                return null;
            }
            return dsFromDB;
        }

        // ✅ User nhập danh sách tên đài (có thể không dấu)
        List<String> dsUserRaw = Arrays.stream(upper.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .toList();

        Set<String> userKeys = dsUserRaw.stream().map(this::daiKey).collect(Collectors.toSet());

        // kiểm tra đài sai
        List<String> dsSai = dsUserRaw.stream()
                .filter(d -> !mapKeyToDisplay.containsKey(daiKey(d)))
                .toList();

        if (!dsSai.isEmpty()) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tên đài không hợp lệ: " + String.join(", ", dsSai)));
            dto.setDanhSachDai(dsFromDB);
            dto.setKetQuaTungDai(List.of());
            return null;
        }

        // giữ thứ tự theo DB
        return dsFromDB.stream()
                .filter(d -> userKeys.contains(daiKey(d)))
                .collect(Collectors.toList());
    }

    private int demComboTrung(List<String> soNguoiChoi, int soXuyen, Set<String> so2CuoiTrongDai) {
        if (soNguoiChoi.size() < soXuyen) return 0;
        int[] count = {0};
        toHop(soNguoiChoi, soXuyen, 0, new ArrayDeque<>(), combo -> {
            boolean ok = combo.stream().allMatch(so2CuoiTrongDai::contains);
            if (ok) count[0]++;
        });
        return count[0];
    }

    private <T> void toHop(List<T> arr, int k, int idx, Deque<T> path, java.util.function.Consumer<List<T>> visit) {
        if (path.size() == k) { visit.accept(List.copyOf(path)); return; }
        for (int i = idx; i <= arr.size() - (k - path.size()); i++) {
            path.addLast(arr.get(i));
            toHop(arr, k, i + 1, path, visit);
            path.removeLast();
        }
    }

    private List<String> parseSoNguoiChoi2ChuSo(String soDanhRaw) {
        if (soDanhRaw == null) return List.of();
        return Arrays.stream(soDanhRaw.split("[-,]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::onlyDigits)
                .filter(s -> !s.isEmpty())
                .map(this::stripLeadingZeros)
                .map(this::pad2)
                .distinct()
                .collect(Collectors.toList());
    }

    private String stripLeadingZeros(String s) {
        String t = s.replaceFirst("^0+(?!$)", "");
        return t.isEmpty() ? "0" : t;
    }

    private String pad2(String s) {
        return (s.length() == 1) ? "0" + s : (s.length() >= 2 ? s.substring(s.length() - 2) : s);
    }

    private String onlyDigits(String s) {
        return s == null ? "" : s.replaceAll("\\D+", "");
    }

    private String daiKey(String s) {
        return removeDiacritics(s == null ? "" : s).toUpperCase().trim().replaceAll("\\s+", "");
    }

    private String xacDinhMienCuaDai(String tenDai, LocalDate ngay) {
        String key = daiKey(tenDai);
        if (bacRepo.findAllByNgay(ngay).stream()
                .anyMatch(kq -> key.equals(daiKey((String) safeGet(kq, "getTenDai"))))) {
            return "MIỀN BẮC";
        }
        if (trungRepo.findAllByNgay(ngay).stream()
                .anyMatch(kq -> key.equals(daiKey((String) safeGet(kq, "getTenDai"))))) {
            return "MIỀN TRUNG";
        }
        if (namRepo.findAllByNgay(ngay).stream()
                .anyMatch(kq -> key.equals(daiKey((String) safeGet(kq, "getTenDai"))))) {
            return "MIỀN NAM";
        }
        return "KHÔNG RÕ";
    }

    private Object safeGet(Object obj, String method) {
        try { return obj.getClass().getMethod(method).invoke(obj); }
        catch (Exception e) { return null; }
    }

    public String chuanHoaCachDanhXuyen(String cachDanh) {
        if (cachDanh == null) return null;
        String cd = removeDiacritics(cachDanh).toUpperCase().replaceAll("[\\s\\.,]+", "");
        cd = cd.replace("XIEN", "XUYEN");
        if (cd.matches("\\d+XUYEN\\d*")) {
            String so = cd.replaceAll("\\D+", "");
            return "XUYEN" + so;
        }
        if (cd.matches("XUYEN\\d+")) return cd;
        if (cd.equals("XUYEN")) return "XUYEN2";
        return cd;
    }

    // ✅ FIX: removeDiacritics phải đổi Đ/đ => D/d thì "3 đài" mới match "\\d+\\s*DAI"
    private String removeDiacritics(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD);
        String noMarks = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noMarks.replace('đ', 'd').replace('Đ', 'D');
    }
}
