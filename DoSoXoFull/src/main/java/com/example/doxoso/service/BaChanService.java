package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class BaChanService {

    @Autowired
    private KetQuaMienBacRepository bacRepo;

    @Autowired
    private KetQuaMienTrungRepository trungRepo;

    @Autowired
    private KetQuaMienNamRepository namRepo;

    @Autowired
    private TinhTienService tinhTienService; // [NEW] dùng để tính tiền 3 CHÂN per-đài

    // =========================================================
    // ================ PUBLIC: XỬ LÝ 3 CHÂN ===================
    // =========================================================
    public DoiChieuKetQuaDto xuLyBaChan(Bet bet) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(bet.getSoDanh());
        dto.setCachTrung("3 chân");

        // [NEW] Validate sớm: số đánh phải đúng 3 chữ số
        if (bet.getSoDanh() == null || !bet.getSoDanh().matches("\\d{3}")) {
            dto.setTrung(false);
            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải đúng 3 chữ số)."));
            return dto;
        }

        // [UPDATED] Lấy danh sách ĐÀI cần dò từ DB, tôn trọng “N ĐÀI”/tên đài người chơi nhập
        List<String> danhSachDai = xacDinhDanhSachDaiCanDo(bet);

        // [UPDATED] Chuẩn hóa miền để kéo đúng dữ liệu trong ngày
        LocalDate ngay = bet.getNgay();
        String mienRaw = bet.getMien() == null ? "" : bet.getMien();
        String mienUpper = removeDiacritics(mienRaw).toUpperCase();

        // [UPDATED] Kéo kết quả theo miền (fallback: tất cả miền nếu không khớp)
        List<Object> ketQuaTongHop = new ArrayList<>();
        if (mienUpper.contains("BAC"))   ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
        if (mienUpper.contains("TRUNG")) ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
        if (mienUpper.contains("NAM"))   ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
        if (ketQuaTongHop.isEmpty()) { // fallback khi miền nhập không rõ
            ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
            ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
            ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
        }

        AtomicBoolean daTrung = new AtomicBoolean(false);

        // [UPDATED] Dò KẾT QUẢ per-đài, chỉ trong danhSachDai đã chọn
        List<DoiChieuKetQuaDto.KetQuaTheoDai> danhSachKetQua = danhSachDai.stream().map(tenDai -> {
            DoiChieuKetQuaDto.KetQuaTheoDai kqDai = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kqDai.setTenDai(tenDai);
            kqDai.setMien(xacDinhMienCuaDai(tenDai, ngay)); // giữ nguyên kiểu trả về "MIỀN BẮC/TRUNG/NAM"

            List<String> giaiTrung = ketQuaTongHop.stream()
                    .filter(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))
                    .filter(kq -> {
                        String soTrung = (String) getField(kq, "getSoTrung");
                        return soTrung != null
                                && soTrung.length() >= 3
                                && soTrung.substring(soTrung.length() - 3).equals(bet.getSoDanh());
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

        // ==============================
        // [UPDATED] TÍNH TIỀN 3 CHÂN (PER-ĐÀI) bằng TinhTienService
        // ==============================
        // [UPDATED] tienChuoi: dùng định dạng "baoLo-thuong-dacBiet" (VD: "10-5-2")
        // Nếu dự án bạn lưu ở field khác, đổi dòng dưới cho đúng:
        String tienChuoi = bet.getSoTien(); // <— [UPDATED] thay cho bet.getSoTien()

        double tongTien = 0, tongBaoLo = 0, tongThuong = 0, tongDacBiet = 0;
        try {
            for (DoiChieuKetQuaDto.KetQuaTheoDai kqDai : danhSachKetQua) {
                if (Boolean.TRUE.equals(kqDai.isTrung())) {
                    double[] tienArr = tinhTienService.tinhTien3Chan(
                            tienChuoi,
                            kqDai.getMien(),       // "MIỀN BẮC/TRUNG/NAM"
                            kqDai.getGiaiTrung()   // danh sách giải trúng của chính đài này
                    );
                    tongTien    += tienArr[0];
                    tongBaoLo   += tienArr[1];
                    tongThuong  += tienArr[2];
                    tongDacBiet += tienArr[3];
                }
            }
        } catch (IllegalArgumentException ex) {
            // Sai định dạng tiền 3 CHÂN
            dto.setTrung(daTrung.get());
            dto.setKetQuaTungDai(danhSachKetQua);
            dto.setDanhSachDai(new ArrayList<>(danhSachDai));
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tiền 3 chân không hợp lệ: " + ex.getMessage()));
            return dto;
        }

        // [UPDATED] Gán kết quả tiền vào DTO
        dto.setTrung(daTrung.get());
        dto.setKetQuaTungDai(danhSachKetQua);
        dto.setDanhSachDai(new ArrayList<>(danhSachDai));
        dto.setTienTrung((double) Math.round(tongTien));
        dto.setTienTrungBaoLo((double) Math.round(tongBaoLo));
        dto.setTienTrungThuong((double) Math.round(tongThuong));
        dto.setTienTrungDacBiet((double) Math.round(tongDacBiet));

        // (tuỳ chọn) Nếu muốn hiển thị tổng tiền CƯỢC bỏ ra:
        // double[] parts = tachTienDanhBaChanLocal(tienChuoi); // baoLo-thuong-dacBiet
        // double tongTienCuocPerDai = parts[0] + parts[1] + parts[2];
        // dto.setTongTienCuoc(tongTienCuocPerDai * danhSachDai.size());

        return dto;
    }

    // =========================================================
    // ===================== HÀM PHỤ TRỢ =======================
    // =========================================================

    // [NEW] Lấy danh sách đài cần dò từ DB, tôn trọng “N ĐÀI”/tên đài nhập
    private List<String> xacDinhDanhSachDaiCanDo(Bet bet) {
        LocalDate ngay = bet.getNgay();
        String m = removeDiacritics(bet.getMien() == null ? "" : bet.getMien()).toUpperCase();

        // Kéo tất cả đài thực tế mở theo NGÀY+MIỀN
        List<Object> ketQuaNgay = new ArrayList<>();
        if (m.contains("BAC"))   ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
        if (m.contains("TRUNG")) ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
        if (m.contains("NAM"))   ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
        if (ketQuaNgay.isEmpty()) { // fallback khi miền không khớp
            ketQuaNgay.addAll(bacRepo.findAllByNgay(ngay));
            ketQuaNgay.addAll(trungRepo.findAllByNgay(ngay));
            ketQuaNgay.addAll(namRepo.findAllByNgay(ngay));
        }

        List<String> dsFromDB = ketQuaNgay.stream()
                .map(kq -> (String) getField(kq, "getTenDai"))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.toList());

        // Đọc input tên đài: ưu tiên bet.getDai() (đồng bộ với service 2 CHÂN)
        String daiInput = bet.getDai();
        if (daiInput == null || daiInput.isBlank()) {
            return dsFromDB;
        }

        String upper = daiInput.trim().toUpperCase();
        String noDia = removeDiacritics(upper);

        // Case "N ĐÀI"/"N DAI"
        if (noDia.matches("\\d+\\s*DAI")) {
            int soDaiMuonDo = Integer.parseInt(noDia.replaceAll("\\D+", ""));
            if (soDaiMuonDo < 1) soDaiMuonDo = 1; // phòng thủ
            if (dsFromDB.size() <= soDaiMuonDo) return dsFromDB;   // không đủ → trả hết
            return dsFromDB.subList(0, soDaiMuonDo);               // đủ → cắt theo thứ tự DB
        }

        // Case user nhập tên đài cụ thể, phân tách bằng dấu phẩy
        List<String> dsUser = Arrays.stream(upper.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toUpperCase).distinct().collect(Collectors.toList());

        // Giữ lại các đài thực sự có trong DB ngày đó (tránh lệch)
        List<String> giao = dsUser.stream().filter(dsFromDB::contains).collect(Collectors.toList());
        return giao.isEmpty() ? dsFromDB : giao;
    }

    // ✅ Hàm phản xạ để gọi getTenDai, getSoTrung, getGiai
    private <T> T getField(Object obj, String methodName) {
        try {
            return (T) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi phản xạ khi lấy field: " + methodName, e);
        }
    }

    // ✅ Xác định miền từ tên đài (dò từ các repo)
    private String xacDinhMienCuaDai(String tenDai, LocalDate ngay) {
        if (bacRepo.findAllByNgay(ngay).stream()
                .anyMatch(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))) {
            return "MIỀN BẮC";
        }
        if (trungRepo.findAllByNgay(ngay).stream()
                .anyMatch(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))) {
            return "MIỀN TRUNG";
        }
        if (namRepo.findAllByNgay(ngay).stream()
                .anyMatch(kq -> tenDai.equalsIgnoreCase((String) getField(kq, "getTenDai")))) {
            return "MIỀN NAM";
        }
        return "KHÔNG RÕ";
    }

    // [NEW] Bỏ dấu để so khớp "BẮC/BAC..." bền vững
    private String removeDiacritics(String input) {
        String normalized = java.text.Normalizer.normalize(input == null ? "" : input, java.text.Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("");
    }

    // (tuỳ chọn) Parser local nếu bạn muốn tính “tổng tiền cược”:
    private double[] tachTienDanhBaChanLocal(String tienDanh) {
        double[] tien = new double[]{0.0, 0.0, 0.0}; // [baoLo, thuong, dacBiet]
        if (tienDanh == null || tienDanh.trim().isEmpty()) return tien;
        String[] parts = tienDanh.trim().split("-");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                try { tien[i] = Double.parseDouble(part); } catch (NumberFormatException ignored) {}
            }
        }
        return tien;
    }
}
