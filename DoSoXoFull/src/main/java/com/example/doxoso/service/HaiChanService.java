package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer; // [NEW]
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class HaiChanService {

    @Autowired
    private KetQuaMienBacRepository bacRepo;

    @Autowired
    private KetQuaMienTrungRepository trungRepo;

    @Autowired
    private KetQuaMienNamRepository namRepo;

    // [NEW] Không dùng config lịch nữa. Tính tiền sẽ làm per-đài.
    @Autowired
    private TinhTienService tinhTienService;

    // =====================================================================
    // ================ LẤY DANH SÁCH ĐÀI TỪ DATABASE (THEO NGÀY/MIỀN) =====
    // =====================================================================

    // [NEW] Lấy danh sách TÊN ĐÀI đang có kết quả trong NGÀY, theo MIỀN nhập.
    // Nếu miền không khớp (không chứa BẮC/TRUNG/NAM) → gom cả 3 miền.
    private List<String> layDanhSachDaiTheoDB(String mienInput, LocalDate ngay) {
        String m = removeDiacritics(mienInput == null ? "" : mienInput).toUpperCase();

        List<Object> ketQuaNgay = new ArrayList<>();
        if (m.contains("BAC"))   ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
        if (m.contains("TRUNG")) ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
        if (m.contains("NAM"))   ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));

        if (ketQuaNgay.isEmpty()) { // fallback: lấy tất cả miền trong NGÀY
            ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
            ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
            ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
        }

        return ketQuaNgay.stream()
                .map(kq -> (String) getField(kq, "getTenDai"))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.toList());
    }

    // =====================================================================
    // ================ XÁC ĐỊNH DANH SÁCH ĐÀI CẦN DÒ (2 CHÂN) =============
    // =====================================================================

    // [UPDATED] Dùng DB làm nguồn sự thật. Hiểu “N ĐÀI” & lọc tên đài user nhập giao với DB.
    public List<String> xacDinhDanhSachDaiCanDo(Bet bet) {
        LocalDate ngay = bet.getNgay();
        String mien = bet.getMien();

        // 1) Lấy danh sách đài THỰC TẾ từ DB theo ngày/miền
        List<String> dsFromDB = layDanhSachDaiTheoDB(mien, ngay);

        // 2) Đọc đầu vào tên đài: ưu tiên bet.getDai() (đúng với model hiện tại)
        String daiInput = bet.getDai();
        if (daiInput != null && !daiInput.isBlank()) {
            String upper = daiInput.trim().toUpperCase();
            String noDia = removeDiacritics(upper);

            // 2.a) “N ĐÀI” / “N DAI”
            if (noDia.matches("\\d+\\s*DAI")) {
                int soDaiMuonDo = Integer.parseInt(noDia.replaceAll("\\D+", ""));
                if (soDaiMuonDo < 1) soDaiMuonDo = 1; // phòng thủ
                if (dsFromDB.size() <= soDaiMuonDo) return dsFromDB;  // không đủ → trả tất cả đang mở
                return dsFromDB.subList(0, soDaiMuonDo);               // đủ → cắt theo thứ tự DB
            }

            // 2.b) Danh sách tên đài cụ thể, phân tách bằng dấu phẩy
            List<String> dsUser = Arrays.stream(upper.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .distinct()
                    .collect(Collectors.toList());

            // Chỉ giữ những đài có thật trong DB ngày đó (tránh lệch nguồn)
            List<String> giao = dsUser.stream()
                    .filter(dsFromDB::contains)
                    .collect(Collectors.toList());

            return giao.isEmpty() ? dsFromDB : giao;
        }

        // 3) Không nhập gì → dùng toàn bộ đài đang mở từ DB
        return dsFromDB;
    }

    // =====================================================================
    // ================== LẤY 2 SỐ CUỐI (PHỤ TRỢ – OPTIONAL) ===============
    // =====================================================================

    public List<String> lay2SoCuoiTrongNgayVaDai(Bet bet) {
        List<String> danhSachDai = xacDinhDanhSachDaiCanDo(bet);
        List<Object> ketQuaTongHop = new ArrayList<>();

        String m = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();
        if (m.contains("BAC"))   ketQuaTongHop.addAll(bacRepo.findAllByNgay(bet.getNgay()));
        if (m.contains("TRUNG")) ketQuaTongHop.addAll(trungRepo.findAllByNgay(bet.getNgay()));
        if (m.contains("NAM"))   ketQuaTongHop.addAll(namRepo.findAllByNgay(bet.getNgay()));
        if (ketQuaTongHop.isEmpty()) { // fallback
            ketQuaTongHop.addAll(bacRepo.findAllByNgay(bet.getNgay()));
            ketQuaTongHop.addAll(trungRepo.findAllByNgay(bet.getNgay()));
            ketQuaTongHop.addAll(namRepo.findAllByNgay(bet.getNgay()));
        }

        return ketQuaTongHop.stream()
                .filter(kq -> {
                    String tenDai = (String) getField(kq, "getTenDai");
                    return tenDai != null && danhSachDai.contains(tenDai.trim().toUpperCase());
                })
                .map(kq -> (String) getField(kq, "getSoTrung"))
                .filter(s -> s != null && s.length() >= 2)
                .map(s -> s.substring(s.length() - 2))
                .collect(Collectors.toList());
    }

    // =====================================================================
    // ================= DÒ & TÍNH TIỀN 2 CHÂN (PER-ĐÀI) ===================
    // =====================================================================

    public DoiChieuKetQuaDto traVeKetQuaChiTiet2Chan(Bet bet) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(bet.getSoDanh());
        dto.setCachTrung("2 chân");

        // [NEW] Validate số đánh 2 chữ số
        if (bet.getSoDanh() == null || !bet.getSoDanh().matches("\\d{2}")) {
            dto.setTrung(false);
            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải đúng 2 chữ số)."));
            return dto;
        }

        // [NEW] Parse tiền đánh per-đài
        double tienDanhPerDai;
        try {
            tienDanhPerDai = Double.parseDouble(bet.getSoTien());
        } catch (Exception e) {
            dto.setTrung(false);
            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ."));
            return dto;
        }

        // 1) Danh sách đài cần dò lấy từ DB (đã expand “N ĐÀI” nếu có)
        List<String> danhSachDai = xacDinhDanhSachDaiCanDo(bet);

        // 2) Kéo kết quả đúng miền trong ngày (fallback nếu miền không rõ)
        List<Object> ketQuaTongHop = new ArrayList<>();
        String m = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();
        if (m.contains("BAC"))   ketQuaTongHop.addAll(bacRepo.findAllByNgay(bet.getNgay()));
        if (m.contains("TRUNG")) ketQuaTongHop.addAll(trungRepo.findAllByNgay(bet.getNgay()));
        if (m.contains("NAM"))   ketQuaTongHop.addAll(namRepo.findAllByNgay(bet.getNgay()));
        if (ketQuaTongHop.isEmpty()) { // fallback
            ketQuaTongHop.addAll(bacRepo.findAllByNgay(bet.getNgay()));
            ketQuaTongHop.addAll(trungRepo.findAllByNgay(bet.getNgay()));
            ketQuaTongHop.addAll(namRepo.findAllByNgay(bet.getNgay()));
        }

        AtomicBoolean daTrung = new AtomicBoolean(false);

        // 3) Dò per-đài
        List<DoiChieuKetQuaDto.KetQuaTheoDai> danhSachKetQua = danhSachDai.stream().map(tenDai -> {
            DoiChieuKetQuaDto.KetQuaTheoDai kqDai = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kqDai.setTenDai(tenDai);
            kqDai.setMien(xacDinhMienCuaDai(tenDai, bet.getNgay()));

            List<String> giaiTrung = ketQuaTongHop.stream()
                    .filter(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))
                    .filter(kq -> {
                        String soTrung = (String) getField(kq, "getSoTrung");
                        if (soTrung != null && soTrung.length() >= 2) {
                            String haiSoCuoi = soTrung.substring(soTrung.length() - 2);
                            return haiSoCuoi.equals(bet.getSoDanh());
                        }
                        return false;
                    })
                    .map(kq -> (String) getField(kq, "getGiai"))
                    .collect(Collectors.toList());

            if (!giaiTrung.isEmpty()) {
                kqDai.setTrung(true);
                kqDai.setSoTrung(bet.getSoDanh());
                kqDai.setGiaiTrung(giaiTrung);
                kqDai.setSoLanTrung(giaiTrung.size());
                daTrung.set(true);
            } else {
                kqDai.setTrung(false);
                kqDai.setLyDo("Không có số trúng " + bet.getSoDanh());
                kqDai.setSoLanTrung(0);
            }
            return kqDai;
        }).collect(Collectors.toList());

        // 4) TÍNH TIỀN 2 CHÂN (per-đài) — dùng TinhTienService
        double tongTien2Chan = 0;
        for (DoiChieuKetQuaDto.KetQuaTheoDai kqDai : danhSachKetQua) {
            if (Boolean.TRUE.equals(kqDai.isTrung())) {
                tongTien2Chan += tinhTienService.tinhTongTien2Chan(
                        kqDai.getMien(),        // MIỀN của ĐÀI này
                        tienDanhPerDai,         // tiền đánh THEO 1 ĐÀI
                        kqDai.getSoLanTrung()   // số lần trúng của ĐÀI này
                );
            }
        }

        // 5) Gán vào DTO
        dto.setTrung(daTrung.get());
        dto.setKetQuaTungDai(danhSachKetQua);
        dto.setDanhSachDai(new ArrayList<>(danhSachDai)); // hiển thị đúng danh sách đã expand
        dto.setTienTrung((double) Math.round(tongTien2Chan));

        return dto;
    }

    // =====================================================================
    // ========================= HÀM PHỤ TRỢ ===============================
    // =====================================================================

    private String xacDinhMienCuaDai(String tenDai, LocalDate ngay) {
        if (bacRepo.findAllByNgay(ngay).stream().anyMatch(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))) {
            return "MIỀN BẮC";
        }
        if (trungRepo.findAllByNgay(ngay).stream().anyMatch(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))) {
            return "MIỀN TRUNG";
        }
        if (namRepo.findAllByNgay(ngay).stream().anyMatch(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))) {
            return "MIỀN NAM";
        }
        return "KHÔNG RÕ";
    }

    private <T> T getField(Object obj, String methodName) {
        try {
            return (T) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi phản xạ khi lấy field: " + methodName);
        }
    }

    // [NEW] Chuẩn hóa bỏ dấu để so khớp miền “BẮC/BAC…”
    private String removeDiacritics(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("");
    }
}
