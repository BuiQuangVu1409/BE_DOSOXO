
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
public class DuoiService {

    @Autowired private KetQuaMienBacRepository bacRepo;
    @Autowired private KetQuaMienTrungRepository trungRepo;
    @Autowired private KetQuaMienNamRepository namRepo;
    @Autowired private TinhTienService tinhTienService;

    public DoiChieuKetQuaDto xuLyDuoi(String soDanh, String mien, LocalDate ngay,
                                      String tienDanh, String tenDai) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(soDanh);
        dto.setMien(mien);
        dto.setNgay(ngay);
        dto.setThu(chuyenNgaySangThu(ngay));
        dto.setCachDanh("ĐUÔI (ĐẶC BIỆT)");
        dto.setCachTrung("ĐUÔI");
        dto.setTienDanh(tienDanh);
        dto.setTenDai(tenDai == null ? "" : tenDai.trim());

        // Validate
        if (soDanh == null || !soDanh.matches("\\d{2}")) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải là 2 chữ số)."));
            return dto;
        }
        if (tienDanh == null) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Thiếu tiền đánh."));
            return dto;
        }

        String mienKey = mienToKey(normalizeNoMarks(mien).toUpperCase()); // MB|MT|MN|UNK
        if ("UNK".equals(mienKey)) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Miền không hợp lệ."));
            return dto;
        }
        final String mienDisplay = switch (mienKey) {
            case "MB" -> "MIỀN BẮC";
            case "MT" -> "MIỀN TRUNG";
            case "MN" -> "MIỀN NAM";
            default -> "KHÔNG RÕ";
        };

        // Đài trong ngày
        List<String> dsDaiTrongNgay = layDanhSachDaiTrongNgayTheoMien(mienKey, ngay);
        if (dsDaiTrongNgay.isEmpty()) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không có đài mở thưởng trong ngày " + ngay + "."));
            return dto;
        }

        // Xác định đài người chơi chọn
        final List<String> dsDaiNguoiChoi;
        String rawTenDai = tenDai == null ? "" : tenDai.trim();
        String tenDaiNoMarks = normalizeNoMarks(rawTenDai).toUpperCase();
        boolean cuPhapNDai = tenDaiNoMarks.matches("\\d+\\s*DAI"); // "2 ĐÀI"...

        if (cuPhapNDai) {
            int soDaiNhap = Integer.parseInt(tenDaiNoMarks.split("\\s+")[0]);
            int soDaiThucTe = dsDaiTrongNgay.size();
            if (soDaiNhap != soDaiThucTe) {
                dto.setTrung(false); dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of("Số đài nhập ("+soDaiNhap+") không khớp số đài hôm nay ("+soDaiThucTe+")."));
                dto.setDanhSachDai(dsDaiTrongNgay);
                return dto;
            }
            dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
        } else {
            if (rawTenDai.isBlank()) {
                if (dsDaiTrongNgay.size() == 1) dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
                else {
                    dto.setTrung(false); dto.setTienTrung(0.0);
                    dto.setSaiLyDo(List.of("Bạn phải nhập tên đài muốn dò, hoặc “N ĐÀI” khớp số đài hôm nay."));
                    dto.setDanhSachDai(dsDaiTrongNgay);
                    return dto;
                }
            } else {
                List<String> dsNhap = Arrays.stream(rawTenDai.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();

                Set<String> dsThucTeKey = dsDaiTrongNgay.stream().map(this::normalizeKeepCase).collect(Collectors.toSet());
                List<String> dsSai = dsNhap.stream()
                        .filter(d -> !dsThucTeKey.contains(normalizeKeepCase(d)))
                        .toList();
                if (!dsSai.isEmpty()) {
                    dto.setTrung(false); dto.setTienTrung(0.0);
                    dto.setSaiLyDo(List.of("Tên đài không hợp lệ: " + String.join(", ", dsSai)));
                    dto.setDanhSachDai(dsDaiTrongNgay);
                    return dto;
                }
                Set<String> dsNhapKey = dsNhap.stream().map(this::normalizeKeepCase).collect(Collectors.toSet());
                dsDaiNguoiChoi = dsDaiTrongNgay.stream()
                        .filter(d -> dsNhapKey.contains(normalizeKeepCase(d)))
                        .collect(Collectors.toList());
            }
        }

        // Tiền MỖI ĐÀI (không chia)
        final double tienPerDai;
        try { tienPerDai = Double.parseDouble(tienDanh); }
        catch (NumberFormatException e) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ."));
            return dto;
        }

        // Dò từng đài: CHỈ GIẢI ĐẶC BIỆT
        List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTungDai = dsDaiNguoiChoi.stream().map(tenDaiChon -> {
            DoiChieuKetQuaDto.KetQuaTheoDai kq = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kq.setTenDai(tenDaiChon);
            kq.setMien(mien);

            List<?> kqDai = getKetQuaTheoDaiVaNgay(mienKey, tenDaiChon, ngay);

            boolean trungDB = kqDai.stream().anyMatch(row -> {
                String giai = safeGetter(row, "getGiai");
                String so   = safeGetter(row, "getSoTrung");
                return isDacBiet(giai) && last2Equals(so, soDanh);
            });

            if (trungDB) {
                kq.setTrung(true);
                kq.setSoTrung(soDanh);
                kq.setGiaiTrung(List.of("ĐẶC BIỆT"));
                kq.setSoLanTrung(1); // chỉ 0/1 vì Đặc Biệt một đài chỉ có 1
                double tienTrungDai = tinhTienService.tinhTienDuoi(true, mienDisplay, tienPerDai);
                kq.setTienTrung(tienTrungDai);
            } else {
                kq.setTrung(false);
                kq.setSoLanTrung(0);
                kq.setTienTrung(0.0);
                kq.setLyDo("Không trúng đuôi ĐẶC BIỆT (" + soDanh + ")");
            }
            return kq;
        }).toList();

        boolean coTrung = ketQuaTungDai.stream().anyMatch(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung);
        double tongTien = ketQuaTungDai.stream().mapToDouble(DoiChieuKetQuaDto.KetQuaTheoDai::getTienTrung).sum();

        dto.setTrung(coTrung);
        dto.setTienTrung(tongTien);
        dto.setKetQuaTungDai(ketQuaTungDai);
        dto.setDanhSachDai(dsDaiNguoiChoi);
        return dto;
    }

    /* ===== Helpers ===== */

    private boolean last2Equals(String soTrung, String soDanh) {
        if (soTrung == null) return false;
        String s = soTrung.trim();
        if (s.length() < 2) return false;
        return s.substring(s.length() - 2).equals(soDanh);
    }

    private boolean isDacBiet(String giai) {
        if (giai == null) return false;
        String g = normalizeKeepCase(giai);
        // tuỳ DB: ĐẶC BIỆT/DB/GĐB/G0...
        return g.equals("DACBIET") || g.equals("DB") || g.equals("GDB") || g.equals("G0");
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
            case "MB" -> bacRepo.findAllByNgay(ngay).stream().filter(kq -> normalizeKeepCase(kq.getTenDai()).equals(keyTen)).toList();
            case "MT" -> trungRepo.findAllByNgay(ngay).stream().filter(kq -> normalizeKeepCase(kq.getTenDai()).equals(keyTen)).toList();
            case "MN" -> namRepo.findAllByNgay(ngay).stream().filter(kq -> normalizeKeepCase(kq.getTenDai()).equals(keyTen)).toList();
            default -> Collections.emptyList();
        };
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
        n = n.replace('Đ','D').replace('đ','d');
        return n.replaceAll("\\p{M}", "");
    }

    private String normalizeKeepCase(String s) {
        return normalizeNoMarks(s).toUpperCase().trim().replaceAll("\\s+", "");
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
        try { Object v = obj.getClass().getMethod(method).invoke(obj); return v == null ? null : v.toString(); }
        catch (Exception e) { return null; }
    }
}


