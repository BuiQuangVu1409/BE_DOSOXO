package com.example.doxoso.service;

import com.example.doxoso.model.DoiChieuKetQuaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DauDuoiService {

    @Autowired private DauService dauService;
    @Autowired private DuoiService duoiService;
    @Autowired private TinhTienService tinhTienService;

    // ===================== PUBLIC API =====================
    public DoiChieuKetQuaDto xuLyDauDuoi(String soDanh, String mien, LocalDate ngay, String tienDanh, String tenDai) {
        DoiChieuKetQuaDto dto = taoDtoCoBan(soDanh, mien, ngay, tienDanh);

        // 1) Validate / chuẩn hoá số đánh: "AA-BB" hoặc "AA" (chấp nhận 1–2 chữ số mỗi phần, tự pad 0)
        soDanh = chuanHoaSoDanh(soDanh);
        if (!hopLeSoDanh(soDanh)) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Số đánh không đúng định dạng A-B hoặc A (vd: 62-15 hoặc 62)"));
            return dto;
        }
        String[] parts = soDanh.contains("-") ? soDanh.split("-") : new String[]{soDanh, soDanh};
        String soDau  = pad2(parts[0]);
        String soDuoi = pad2(parts[1]);

        // 2) Tiền/đài (không chia theo số đài)
        double tienPerDai = parseTienDanh(tienDanh, dto);
        if (tienPerDai <= 0) return dto;

        // 3) Gọi sub-services:
        //    - ĐẦU: truyền NỬA tiền/đài (đúng nguyên tắc đầu-đuôi chia đôi trên 1 đài)
        //    - ĐUÔI: truyền NỬA tiền/đài
        DoiChieuKetQuaDto kqDau  = dauService.xuLyDau (soDau,  mien, ngay, String.valueOf(tienPerDai / 2.0), tenDai);
        DoiChieuKetQuaDto kqDuoi = duoiService.xuLyDuoi(soDuoi, mien, ngay, String.valueOf(tienPerDai / 2.0), tenDai);

        // Nếu có lỗi cứng từ sub-service, ưu tiên trả ra sớm
        if (laLoiCung(kqDau) && laLoiCung(kqDuoi)) {
            dto.setSaiLyDo(Stream.concat(
                    Optional.ofNullable(kqDau.getSaiLyDo()).orElse(List.of()).stream(),
                    Optional.ofNullable(kqDuoi.getSaiLyDo()).orElse(List.of()).stream()
            ).distinct().toList());
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            return dto;
        }
        if (laLoiCung(kqDau))  return kqDau;
        if (laLoiCung(kqDuoi)) return kqDuoi;

        // 4) Gom danh sách đài người chơi chọn (union từ 2 kết quả)
        List<String> danhSachDai = Stream.concat(
                Optional.ofNullable(kqDau.getDanhSachDai()).orElse(List.of()).stream(),
                Optional.ofNullable(kqDuoi.getDanhSachDai()).orElse(List.of()).stream()
        ).distinct().toList();
        dto.setDanhSachDai(danhSachDai);

        // 5) Tạo chi tiết từng đài + tính tiền per-đài, rồi SUM lại
        String mienDisplay = toMienDisplay(mien); // chuẩn "MIỀN BẮC|TRUNG|NAM"
        List<DoiChieuKetQuaDto.KetQuaTheoDai> chiTiet = new ArrayList<>();
        double tongTien = 0.0;

        for (String dai : danhSachDai) {
            boolean trungDauDai = coTrungTheoDai(kqDau, dai);
            boolean trungDuoiDai = coTrungTheoDai(kqDuoi, dai);

            // Per-đài: nếu trúng cả 2 → chia đôi tiền đài ngay bên trong tinhTienDauDuoi()
            double tienTrungDai = tinhTienService.tinhTienDauDuoi(
                    trungDauDai, trungDuoiDai,
                    mienDisplay, mienDisplay,
                    tienPerDai
            );
            tongTien += tienTrungDai;

            // Ghép text số trúng cho hiển thị
            String soTrungText = Stream.of(
                    trungDauDai  ? "ĐẦU: "  + laySoTheoDai(kqDau, dai)  : null,
                    trungDuoiDai ? "ĐUÔI: " + laySoTheoDai(kqDuoi, dai) : null
            ).filter(Objects::nonNull).collect(Collectors.joining(" | "));

            DoiChieuKetQuaDto.KetQuaTheoDai kq = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kq.setTenDai(dai);
            kq.setMien(mien);
            kq.setSoTrung(soTrungText.isEmpty() ? null : soTrungText);
            kq.setTienTrung(tienTrungDai);
            kq.setTrung(tienTrungDai > 0);
            chiTiet.add(kq);
        }

        dto.setKetQuaTungDai(chiTiet);
        dto.setTienTrung(tongTien);

        // 6) Cách trúng tổng hợp
        String soTrungDauAll  = joinSoTrung(kqDau);
        String soTrungDuoiAll = joinSoTrung(kqDuoi);
        boolean coTrungDau  = chiTiet.stream().anyMatch(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung) &&
                kqDau.isTrung();
        boolean coTrungDuoi = chiTiet.stream().anyMatch(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung) &&
                kqDuoi.isTrung();

        if (tongTien > 0) {
            dto.setTrung(true);
            if (coTrungDau && coTrungDuoi) {
                dto.setGiaiTrung("Trúng cả ĐẦU và ĐUÔI");
                dto.setCachTrung("ĐẦU: " + soTrungDauAll + ", ĐUÔI: " + soTrungDuoiAll);
            } else if (coTrungDau) {
                dto.setGiaiTrung("Chỉ trúng ĐẦU");
                dto.setCachTrung("ĐẦU: " + soTrungDauAll);
                dto.setSaiLyDo(List.of("Chỉ trúng đầu, không trúng đuôi"));
            } else { // coTrungDuoi
                dto.setGiaiTrung("Chỉ trúng ĐUÔI");
                dto.setCachTrung("ĐUÔI: " + soTrungDuoiAll);
                dto.setSaiLyDo(List.of("Chỉ trúng đuôi, không trúng đầu"));
            }
        } else {
            dto.setTrung(false);
            dto.setSaiLyDo(List.of("Không trúng đầu hoặc đuôi"));
        }

        // Tên đài hiển thị
        dto.setTenDai(Stream.of(kqDau.getTenDai(), kqDuoi.getTenDai())
                .filter(Objects::nonNull).distinct().collect(Collectors.joining(", ")));

        return dto;
    }

    // ===================== HELPERS =====================
    private DoiChieuKetQuaDto taoDtoCoBan(String soDanh, String mien, LocalDate ngay, String tienDanh) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(soDanh);
        dto.setMien(mien);
        dto.setNgay(ngay);
        dto.setThu(chuyenNgaySangThu(ngay));
        dto.setCachDanh("ĐẦU ĐUÔI");
        dto.setTienDanh(tienDanh);
        return dto;
    }

    private String chuyenNgaySangThu(LocalDate ngay) {
        return switch (ngay.getDayOfWeek()) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }

    private String chuanHoaSoDanh(String so) {
        return so == null ? null : so.trim().replaceAll("\\s+", "");
    }

    private boolean hopLeSoDanh(String so) {
        // "A"|"AA" hoặc "A-B"|"AA-BB"
        return so != null && so.matches("\\d{1,2}(-\\d{1,2})?");
    }

    private String pad2(String s) {
        return (s.length() == 1) ? "0" + s : s;
    }

    private double parseTienDanh(String tienDanh, DoiChieuKetQuaDto dto) {
        try {
            return Double.parseDouble(tienDanh);
        } catch (Exception e) {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ"));
            return -1;
        }
    }

    private boolean laLoiCung(DoiChieuKetQuaDto kq) {
        return kq != null && !kq.isTrung() && kq.getTienTrung() == 0.0 && kq.getSaiLyDo() != null && !kq.getSaiLyDo().isEmpty();
    }

    private boolean coTrungTheoDai(DoiChieuKetQuaDto sub, String dai) {
        if (sub == null || sub.getKetQuaTungDai() == null) return false;
        return sub.getKetQuaTungDai().stream()
                .anyMatch(k -> dai.equals(k.getTenDai()) && k.isTrung());
    }

    private String laySoTheoDai(DoiChieuKetQuaDto sub, String dai) {
        if (sub == null || sub.getKetQuaTungDai() == null) return null;
        return sub.getKetQuaTungDai().stream()
                .filter(k -> dai.equals(k.getTenDai()) && k.isTrung())
                .map(DoiChieuKetQuaDto.KetQuaTheoDai::getSoTrung)
                .findFirst().orElse(null);
    }

    private String joinSoTrung(DoiChieuKetQuaDto sub) {
        if (sub == null || sub.getKetQuaTungDai() == null) return "";
        return sub.getKetQuaTungDai().stream()
                .map(k -> k.getSoTrung() != null ? k.getTenDai() + ": " + k.getSoTrung() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" | "));
    }

    private String toMienDisplay(String raw) {
        if (raw == null) return "MIỀN NAM"; // default an toàn
        String u = raw.trim().toUpperCase();
        if (u.contains("BẮC") || u.contains("BAC") || u.equals("MB"))  return "MIỀN BẮC";
        if (u.contains("TRUNG") || u.equals("MT"))                      return "MIỀN TRUNG";
        return "MIỀN NAM"; // u.contains("NAM") || u.equals("MN")
    }
}
