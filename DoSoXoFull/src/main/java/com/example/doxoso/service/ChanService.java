package com.example.doxoso.service;

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
public class ChanService {

    @Autowired private KetQuaMienBacRepository bacRepo;
    @Autowired private KetQuaMienTrungRepository trungRepo;
    @Autowired private KetQuaMienNamRepository namRepo;

    @Autowired private TinhTienService tinhTienService;

    /**
     * loaiChan: "CHAN_DAU" | "CHAN_DUOI" | "CHẴN ĐẦU" | "CHẴN ĐUÔI" ...
     * tienDanh: TIỀN/ĐÀI (KHÔNG chia)
     * tenDai: "N ĐÀI" hoặc "Đài A, Đài B" hoặc bỏ trống nếu ngày đó chỉ có 1 đài
     */
    public DoiChieuKetQuaDto xuLyChan(Long playerId, String loaiChan, String mien, LocalDate ngay, String tienDanh, String tenDai) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(""); // CHẴN không cần nhập số
        dto.setMien(mien);
        dto.setNgay(ngay);
        dto.setThu(chuyenNgaySangThu(ngay));
        dto.setCachDanh("CHẴN");
        dto.setTienDanh(tienDanh);
        dto.setTenDai(tenDai == null ? "" : tenDai.trim());

        if (playerId == null) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Thiếu playerId."));
            return dto;
        }

        if (tienDanh == null || tienDanh.trim().isEmpty()) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Thiếu tiền đánh."));
            return dto;
        }

        // ===== Xác định loại chẵn =====
        String loai = normalizeNoMarks(loaiChan).toUpperCase().trim();
        boolean isChanDau = loai.contains("DAU");
        boolean isChanDuoi = loai.contains("DUOI");
        if (!isChanDau && !isChanDuoi) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Loại chẵn không hợp lệ. Dùng CHẴN ĐẦU hoặc CHẴN ĐUÔI."));
            return dto;
        }
        dto.setCachTrung(isChanDau ? "CHẴN ĐẦU" : "CHẴN ĐUÔI");

        // ===== Miền =====
        String mienKey = mienToKey(normalizeNoMarks(mien).toUpperCase());
        if ("UNK".equals(mienKey)) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Miền không hợp lệ."));
            return dto;
        }

        // MB không có CHẴN ĐẦU
        if (isChanDau && "MB".equals(mienKey)) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("MIỀN BẮC không có CHẴN ĐẦU, chỉ có CHẴN ĐUÔI."));
            return dto;
        }

        // ===== DS đài trong ngày =====
        List<String> dsDaiTrongNgay = layDanhSachDaiTrongNgayTheoMien(mienKey, ngay);
        if (dsDaiTrongNgay.isEmpty()) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không có đài mở thưởng trong ngày " + ngay + "."));
            return dto;
        }

        // ===== DS đài người chơi chọn =====
        final List<String> dsDaiNguoiChoi = parseDaiNguoiChoi(dto, dsDaiTrongNgay, tenDai);
        if (dsDaiNguoiChoi == null) return dto;

        // ===== Validate tiền (chỉ để chắc chắn là số) =====
        try {
            Double.parseDouble(tienDanh.trim());
        } catch (NumberFormatException e) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ."));
            return dto;
        }

        // ===== Dò từng đài =====
        List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTungDai = dsDaiNguoiChoi.stream().map(tenDaiChon -> {
            DoiChieuKetQuaDto.KetQuaTheoDai kq = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kq.setTenDai(tenDaiChon);
            kq.setMien(mien);

            List<?> rows = getKetQuaTheoDaiVaNgay(mienKey, tenDaiChon, ngay);

            // ===== Lọc đúng giải cần check =====
            List<Object> rowsCanCheck = rows.stream()
                    .filter(r -> {
                        String giai = safeGetter(r, "getGiai");
                        if (giai == null) return false;

                        if (isChanDau) {
                            // CHẴN ĐẦU: MT/MN check G8
                            return "G8".equalsIgnoreCase(giai.trim());
                        } else {
                            // CHẴN ĐUÔI: check ĐẶC BIỆT
                            return isDacBiet(giai);
                        }
                    })
                    .map(r -> (Object) r)
                    .collect(Collectors.toList());

            // ===== Lấy 2 số cuối, lọc chẵn, distinct tránh trùng =====
            List<String> so2CuoiTrung = rowsCanCheck.stream()
                    .map(r -> safeGetter(r, "getSoTrung"))
                    .filter(Objects::nonNull)
                    .map(this::onlyDigits)
                    .filter(s -> s.length() >= 2)
                    .map(this::last2)
                    .filter(two -> {
                        try { return Integer.parseInt(two) % 2 == 0; }
                        catch (Exception e) { return false; }
                    })
                    .distinct()
                    .collect(Collectors.toList());

            if (!so2CuoiTrung.isEmpty()) {
                int soLanTrung = so2CuoiTrung.size();

                kq.setTrung(true);
                kq.setSoLanTrung(soLanTrung);
                kq.setSoTrung(String.join(", ", so2CuoiTrung));
                kq.setGiaiTrung(List.of(isChanDau ? "G8" : "ĐẶC BIỆT"));

                // ✅ Gọi TinhTienService đúng chữ ký (tienDanhPerDai là String)
                double tienTrungDai = isChanDau
                        ? tinhTienService.tinhTienChanDau(true, playerId, tienDanh.trim(), soLanTrung)
                        : tinhTienService.tinhTienChanDuoi(true, playerId, tienDanh.trim(), soLanTrung);

                kq.setTienTrung(tienTrungDai);
            } else {
                kq.setTrung(false);
                kq.setSoLanTrung(0);
                kq.setTienTrung(0.0);
                kq.setLyDo(isChanDau
                        ? "G8 không có 2 số cuối chẵn"
                        : "Đặc Biệt không có 2 số cuối chẵn");
            }

            return kq;
        }).collect(Collectors.toList());

        boolean coTrung = ketQuaTungDai.stream().anyMatch(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung);
        double tongTien = ketQuaTungDai.stream().mapToDouble(DoiChieuKetQuaDto.KetQuaTheoDai::getTienTrung).sum();

        dto.setTrung(coTrung);
        dto.setTienTrung(tongTien);
        dto.setKetQuaTungDai(ketQuaTungDai);
        dto.setDanhSachDai(dsDaiNguoiChoi);

        return dto;
    }

    // ================= Helpers =================

    private List<String> parseDaiNguoiChoi(DoiChieuKetQuaDto dto, List<String> dsDaiTrongNgay, String tenDai) {
        final String rawTenDai = tenDai == null ? "" : tenDai.trim();
        final String tenDaiNoMarks = normalizeNoMarks(rawTenDai).toUpperCase();
        boolean cuPhapNDai = tenDaiNoMarks.matches("\\d+\\s*DAI"); // "3 DAI" / "3 ĐÀI"

        if (cuPhapNDai) {
            int soDaiNhap = Integer.parseInt(tenDaiNoMarks.split("\\s+")[0]);
            int soDaiThucTe = dsDaiTrongNgay.size();
            if (soDaiNhap != soDaiThucTe) {
                dto.setTrung(false); dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of("Số đài nhập (" + soDaiNhap + ") không khớp số đài mở thưởng hôm nay (" + soDaiThucTe + ")."));
                dto.setDanhSachDai(dsDaiTrongNgay);
                return null;
            }
            return new ArrayList<>(dsDaiTrongNgay);
        }

        if (rawTenDai.isBlank()) {
            if (dsDaiTrongNgay.size() == 1) return new ArrayList<>(dsDaiTrongNgay);
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Bạn phải nhập tên đài muốn dò, hoặc “N ĐÀI” khớp số đài hôm nay."));
            dto.setDanhSachDai(dsDaiTrongNgay);
            return null;
        }

        List<String> dsNhap = Arrays.stream(rawTenDai.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Set<String> dsThucTeKey = dsDaiTrongNgay.stream().map(this::normalizeKeepCase).collect(Collectors.toSet());
        List<String> dsSai = dsNhap.stream()
                .filter(d -> !dsThucTeKey.contains(normalizeKeepCase(d)))
                .collect(Collectors.toList());

        if (!dsSai.isEmpty()) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tên đài không hợp lệ: " + String.join(", ", dsSai)));
            dto.setDanhSachDai(dsDaiTrongNgay);
            return null;
        }

        // Giữ đúng tên/format & thứ tự theo DB
        Set<String> dsNhapKey = dsNhap.stream().map(this::normalizeKeepCase).collect(Collectors.toSet());
        return dsDaiTrongNgay.stream()
                .filter(d -> dsNhapKey.contains(normalizeKeepCase(d)))
                .collect(Collectors.toList());
    }

    private List<String> layDanhSachDaiTrongNgayTheoMien(String mienKey, LocalDate ngay) {
        return switch (mienKey) {
            case "MB" -> bacRepo.findAllByNgay(ngay).stream().map(kq -> kq.getTenDai().trim()).distinct().toList();
            case "MT" -> trungRepo.findAllByNgay(ngay).stream().map(kq -> kq.getTenDai().trim()).distinct().toList();
            case "MN" -> namRepo.findAllByNgay(ngay).stream().map(kq -> kq.getTenDai().trim()).distinct().toList();
            default -> Collections.emptyList();
        };
    }

    private List<?> getKetQuaTheoDaiVaNgay(String mienKey, String tenDai, LocalDate ngay) {
        String keyTen = normalizeKeepCase(tenDai);
        return switch (mienKey) {
            case "MB" -> bacRepo.findAllByNgay(ngay).stream()
                    .filter(kq -> normalizeKeepCase(kq.getTenDai()).equals(keyTen)).toList();
            case "MT" -> trungRepo.findAllByNgay(ngay).stream()
                    .filter(kq -> normalizeKeepCase(kq.getTenDai()).equals(keyTen)).toList();
            case "MN" -> namRepo.findAllByNgay(ngay).stream()
                    .filter(kq -> normalizeKeepCase(kq.getTenDai()).equals(keyTen)).toList();
            default -> Collections.emptyList();
        };
    }

    private boolean isDacBiet(String giaiRaw) {
        String g = normalizeNoMarks(giaiRaw).toUpperCase().trim().replaceAll("\\s+", "");
        return g.equals("DB") || g.contains("DACBIET");
    }

    private String mienToKey(String mienNoMarksUpper) {
        if (mienNoMarksUpper.contains("BAC") || "MB".equals(mienNoMarksUpper)) return "MB";
        if (mienNoMarksUpper.contains("TRUNG") || "MT".equals(mienNoMarksUpper)) return "MT";
        if (mienNoMarksUpper.contains("NAM") || "MN".equals(mienNoMarksUpper)) return "MN";
        return "UNK";
    }

    private String normalizeNoMarks(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replace('Đ', 'D').replace('đ', 'd');
        return n.replaceAll("\\p{M}", "");
    }

    private String normalizeKeepCase(String s) {
        return normalizeNoMarks(s).toUpperCase().trim().replaceAll("\\s+", "");
    }

    private String onlyDigits(String s) {
        return (s == null) ? "" : s.replaceAll("\\D+", "");
    }

    private String last2(String digits) {
        int n = digits.length();
        return digits.substring(n - 2);
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

    private String safeGetter(Object obj, String method) {
        try {
            Object v = obj.getClass().getMethod(method).invoke(obj);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
