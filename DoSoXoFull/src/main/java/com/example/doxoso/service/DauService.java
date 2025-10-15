package com.example.doxoso.service;

import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DauService {

    @Autowired private KetQuaMienBacRepository bacRepo;
    @Autowired private KetQuaMienTrungRepository trungRepo;
    @Autowired private KetQuaMienNamRepository namRepo;
    @Autowired private TinhTienService tinhTienService;

    /**
     * Nghiệp vụ ĐẦU:
     *  - MB: so khớp G7 (2 số)
     *  - MT/MN: so khớp G8 (2 số)
     *  - Hỗ trợ "N ĐÀI" hoặc danh sách "Đài A, Đài B"
     *  - Tiền nhập là TIỀN/ĐÀI (KHÔNG chia đều)
     */
    public DoiChieuKetQuaDto xuLyDau(String soDanh, String mien, LocalDate ngay, String tienDanh, String tenDai) {
        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setSoDanh(soDanh);
        dto.setMien(mien);
        dto.setNgay(ngay);
        dto.setThu(chuyenNgaySangThu(ngay));
        dto.setCachDanh("ĐẦU");
        dto.setCachTrung("ĐẦU");
        dto.setTienDanh(tienDanh);
        dto.setTenDai(tenDai == null ? "" : tenDai.trim());

        // ===== Validate cơ bản =====
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

        // Miền → key/label dùng tính tiền
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

        // Lấy DS đài thực tế trong ngày (giữ nguyên format DB để hiển thị)
        List<String> dsDaiTrongNgay = layDanhSachDaiTrongNgayTheoMien(mienKey, ngay);
        if (dsDaiTrongNgay.isEmpty()) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không có đài mở thưởng trong ngày " + ngay + "."));
            return dto;
        }

        // ===== Xác định đài người chơi chọn =====
        final List<String> dsDaiNguoiChoi;
        final String rawTenDai = tenDai == null ? "" : tenDai.trim();
        final String tenDaiNoMarks = normalizeNoMarks(rawTenDai).toUpperCase();
        boolean cuPhapNDai = tenDaiNoMarks.matches("\\d+\\s*DAI"); // "3 ĐAI/ĐÀI"

        if (cuPhapNDai) {
            int soDaiNhap = Integer.parseInt(tenDaiNoMarks.split("\\s+")[0]);
            int soDaiThucTe = dsDaiTrongNgay.size();
            if (soDaiNhap != soDaiThucTe) {
                dto.setTrung(false); dto.setTienTrung(0.0);
                dto.setSaiLyDo(List.of("Số đài nhập ("+soDaiNhap+") không khớp số đài mở thưởng hôm nay ("+soDaiThucTe+")."));
                dto.setDanhSachDai(dsDaiTrongNgay);
                return dto;
            }
            dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
        } else {
            if (rawTenDai.isBlank()) {
                if (dsDaiTrongNgay.size() == 1) {
                    dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
                } else {
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
                // Giữ đúng tên/format & thứ tự theo DB
                Set<String> dsNhapKey = dsNhap.stream().map(this::normalizeKeepCase).collect(Collectors.toSet());
                dsDaiNguoiChoi = dsDaiTrongNgay.stream()
                        .filter(d -> dsNhapKey.contains(normalizeKeepCase(d)))
                        .collect(Collectors.toList());
            }
        }

        // ===== Parse tiền: TIỀN/ĐÀI (KHÔNG CHIA) =====
        final double tienPerDai;
        try {
            tienPerDai = Double.parseDouble(tienDanh);
        } catch (NumberFormatException e) {
            dto.setTrung(false); dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ."));
            return dto;
        }

        // Giải cần so khớp theo miền
        final String giaiRequired = "MB".equals(mienKey) ? "G7" : "G8";

        // ===== Dò từng đài =====
        List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTungDai = dsDaiNguoiChoi.stream().map(tenDaiChon -> {
            DoiChieuKetQuaDto.KetQuaTheoDai kq = new DoiChieuKetQuaDto.KetQuaTheoDai();
            kq.setTenDai(tenDaiChon);
            kq.setMien(mien);

            // KQ theo đài/ngày
            List<?> kqDai = getKetQuaTheoDaiVaNgay(mienKey, tenDaiChon, ngay);

            // Lọc đúng giải & so sánh "đầu" 2 số: phải BẰNG nhau
            List<String> giaiTrung = kqDai.stream()
                    .filter(row -> {
                        String giai = safeGetter(row, "getGiai");
                        String so = safeGetter(row, "getSoTrung");
                        if (giai == null || so == null) return false;
                        if (!giaiRequired.equalsIgnoreCase(giai.trim())) return false;
                        String val = so.trim();
                        return val.length() == 2 && val.equals(soDanh);
                    })
                    .map(row -> (String) getField(row, "getGiai"))
                    .distinct()
                    .collect(Collectors.toList());

            if (!giaiTrung.isEmpty()) {
                kq.setTrung(true);
                kq.setSoTrung(soDanh);
                kq.setGiaiTrung(giaiTrung);
                kq.setSoLanTrung(giaiTrung.size());
                // TIỀN/ĐÀI * hệ số theo miền * số_lần_trúng_đài
                double tienTrungDai = tinhTienService.tinhTienDau(true, mienDisplay, tienPerDai) * kq.getSoLanTrung();
                kq.setTienTrung(tienTrungDai);
            } else {
                kq.setTrung(false);
                kq.setSoLanTrung(0);
                kq.setTienTrung(0.0);
                kq.setLyDo("Không có số trúng " + soDanh);
            }
            return kq;
        }).toList();

        boolean coTrung = ketQuaTungDai.stream().anyMatch(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung);
        double tongTien = ketQuaTungDai.stream().mapToDouble(DoiChieuKetQuaDto.KetQuaTheoDai::getTienTrung).sum();

        dto.setTrung(coTrung);
        dto.setTienTrung(tongTien);
        dto.setKetQuaTungDai(ketQuaTungDai);
        dto.setDanhSachDai(dsDaiNguoiChoi); // trả về đúng đài người chơi chọn
        return dto;
    }

    // ===== Helpers =====

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

    private String mienToKey(String mienNoMarksUpper) {
        if (mienNoMarksUpper.contains("BAC") || "MB".equals(mienNoMarksUpper))  return "MB";
        if (mienNoMarksUpper.contains("TRUNG") || "MT".equals(mienNoMarksUpper)) return "MT";
        if (mienNoMarksUpper.contains("NAM") || "MN".equals(mienNoMarksUpper))  return "MN";
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
        try { Object v = getField(obj, method); return v == null ? null : v.toString(); }
        catch (Exception e) { return null; }
    }
    @SuppressWarnings("unchecked")
    private <T> T getField(Object obj, String methodName) {
        try { return (T) obj.getClass().getMethod(methodName).invoke(obj); }
        catch (Exception e) { throw new RuntimeException("Lỗi phản xạ khi lấy field: " + methodName, e); }
    }
}



////        public DoiChieuKetQuaDto xuLyDau(String soDanh, String mien, LocalDate ngay, String tienDanh) {
////            DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
////            dto.setSoDanh(soDanh);
////            dto.setMien(mien);
////            dto.setNgay(ngay);
////            dto.setThu(chuyenNgaySangThu(ngay));
////            dto.setCachDanh("DAU");
////            dto.setTienDanh(tienDanh);
////            dto.setDanhSachDai(new ArrayList<>());
////
////            if (tienDanh == null) {
////                dto.setTrung(false);
////                dto.setTienTrung(0);
////                dto.setSaiLyDo(List.of("Thiếu tiền đánh"));
////                return dto;
////            }
////
////            if (!soDanh.matches("\\d{2}")) {
////                dto.setTrung(false);
////                dto.setTienTrung(0);
////                dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải là 2 chữ số)"));
////                return dto;
////            }
////
////            String soChuan = chuanHoa(soDanh);
////            String mienChuan = chuanHoa(mien);
////            List<String> saiLyDo = new ArrayList<>();
////
////            if (mienChuan.equals("MIENBAC")) {
////                var ketQua = bacRepo.findAllByNgay(ngay).stream()
////
////                        .filter(kq -> kq.getGiai() != null && "G7".equalsIgnoreCase(kq.getGiai()))
////
////                        .toList();
////
////                Set<String> tatCaDai = new HashSet<>();
////                for (var kq : ketQua) {
////                    tatCaDai.add(kq.getTenDai());
////
////                    // So sánh trực tiếp từng dòng G7 – mỗi dòng là 1 cặp số như "78"
////                    if (soChuan.equals(chuanHoa(kq.getSoTrung()))) {
////                        dto.setTrung(true);
////                        dto.setGiaiTrung("Giải 7");
////                        dto.setTenDai(kq.getTenDai());
////                        dto.setCachTrung("Trúng ĐẦU MIỀN BẮC – Đài " + kq.getTenDai());
////                        dto.setTienTrung(Double.parseDouble(tienDanh) * 1000);
////                        dto.setDanhSachDai(List.copyOf(tatCaDai));
////                        return dto;
////                    }
////                }
////
////                dto.setTrung(false);
////                dto.setTienTrung(0);
////                dto.setSaiLyDo(List.of("Không trúng ĐẦU MIỀN BẮC – Giải 7"));
////                dto.setDanhSachDai(List.copyOf(tatCaDai));
////                return dto;
////            }
////
////            if (mienChuan.equals("MIENTRUNG")) {
////                var ketQua = trungRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> kq.getGiai() != null && "G8".equalsIgnoreCase(kq.getGiai()))
////                        .toList();
////                for (var kq : ketQua) {
////                    dto.getDanhSachDai().add(kq.getTenDai());
////                    if (soChuan.equals(chuanHoa(kq.getSoTrung()))) {
////                        dto.setTrung(true);
////                        dto.setGiaiTrung("Giải 8");
////                        dto.setTenDai(kq.getTenDai());
////                        dto.setCachTrung("Trúng ĐẦU MIỀN TRUNG – Đài " + kq.getTenDai());
////                        dto.setTienTrung(Double.parseDouble(tienDanh) * 1000);
////                        return dto;
////                    } else {
////                        saiLyDo.add("Trật đài " + kq.getTenDai());
////                    }
////                }
////            }
////
////            if (mienChuan.equals("MIENNAM")) {
////                var ketQua = namRepo.findAllByNgay(ngay).stream()
////
////                        .filter(kq -> kq.getGiai() != null && "G8".equalsIgnoreCase(kq.getGiai()))
////                        .toList();
////
////
////                for (var kq : ketQua) {
////                    dto.getDanhSachDai().add(kq.getTenDai());
////                    if (soChuan.equals(chuanHoa(kq.getSoTrung()))) {
////                        dto.setTrung(true);
////                        dto.setGiaiTrung("Giải 8");
////                        dto.setTenDai(kq.getTenDai());
////                        dto.setCachTrung("Trúng ĐẦU MIỀN NAM – Đài " + kq.getTenDai());
////                        dto.setTienTrung(Double.parseDouble(tienDanh) * 1000);
////                        return dto;
////                    } else {
////                        saiLyDo.add("Trật đài " + kq.getTenDai());
////                    }
////                }
////            }
////
////            dto.setTrung(false);
////            dto.setTienTrung(0);
////            dto.setSaiLyDo(saiLyDo.isEmpty() ? List.of("Không trúng số nào") : saiLyDo);
////            return dto;
////        }
////
//
////            public DoiChieuKetQuaDto xuLyDau(String soDanh, String mien, LocalDate ngay, String tienDanh, String tenDai) {
////                DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
////                dto.setSoDanh(soDanh);
////                dto.setTenDai(tenDai);
////                dto.setMien(mien);
////                dto.setNgay(ngay);
////                dto.setThu(chuyenNgaySangThu(ngay));
////                dto.setCachDanh("DAU");
////                dto.setTienDanh(tienDanh);
////                dto.setDanhSachDai(new ArrayList<>());
////
////                // Kiểm tra đầu vào
////                if (soDanh == null || tienDanh == null) {
////                    dto.setTrung(false);
////                    dto.setTienTrung(0.0);
////                    dto.setSaiLyDo(List.of("Thiếu thông tin bắt buộc (số đánh hoặc tiền đánh)"));
////                    return dto;
////                }
////                if (!soDanh.matches("\\d{2}")) {
////                    dto.setTrung(false);
////                    dto.setTienTrung(0.0);
////                    dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải là 2 chữ số)"));
////                    return dto;
////                }
////
////                String soChuan = chuanHoa(soDanh);
////                String mienChuan = chuanHoa(mien);
////                List<String> dsDaiTrongNgay = layDanhSachDaiTrongNgay(mienChuan, ngay);
////
////                if (dsDaiTrongNgay.isEmpty()) {
////                    dto.setTrung(false);
////                    dto.setTienTrung(0.0);
////                    dto.setSaiLyDo(List.of("Không có đài mở thưởng trong ngày " + ngay));
////                    return dto;
////                }
////
////                String tenDaiRaw = tenDai == null ? "" : tenDai.trim().toUpperCase();
////                boolean isNhapDangXDAI = tenDaiRaw.matches("\\d\\s*ĐÀI");
////                List<String> dsDaiNguoiChoi;
////                int soDaiThucTe = dsDaiTrongNgay.size();
////
////                if (isNhapDangXDAI) {
////                    int soDaiNguoiNhap = Integer.parseInt(tenDaiRaw.split("\\s")[0]);
////
////                    if (soDaiNguoiNhap == soDaiThucTe) {
////                        dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
////                    } else if (soDaiNguoiNhap < soDaiThucTe) {
////                        dto.setTrung(false);
////                        dto.setTienTrung(0.0);
////                        dto.setSaiLyDo(List.of("Hôm nay có " + soDaiThucTe + " đài, bạn cần ghi rõ tên " + soDaiNguoiNhap + " đài muốn đánh!"));
////                        return dto;
////                    } else {
////                        dto.setTrung(false);
////                        dto.setTienTrung(0.0);
////                        dto.setSaiLyDo(List.of("Hôm nay chỉ có " + soDaiThucTe + " đài, bạn không thể đánh " + soDaiNguoiNhap + " đài!"));
////                        return dto;
////                    }
////                } else {
////                    dsDaiNguoiChoi = Arrays.stream(tenDaiRaw.split("\\s*,\\s*"))
////                            .filter(s -> !s.isBlank())
////                            .map(this::chuanHoa)
////                            .toList();
////
////                    if (dsDaiNguoiChoi.isEmpty()) {
////                        if (soDaiThucTe == 1) {
////                            dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
////                        } else {
////                            dto.setTrung(false);
////                            dto.setTienTrung(0.0);
////                            dto.setSaiLyDo(List.of("Hôm nay có " + soDaiThucTe + " đài, bạn phải nhập rõ tên đài muốn dò"));
////                            return dto;
////                        }
////                    }
////                }
////
////                // Kiểm tra tên đài hợp lệ
////                List<String> dsSai = dsDaiNguoiChoi.stream()
////                        .filter(dai -> !dsDaiTrongNgay.contains(dai))
////                        .toList();
////
////                if (!dsSai.isEmpty()) {
////                    dto.setTrung(false);
////                    dto.setTienTrung(0.0);
////                    dto.setSaiLyDo(List.of(
////                            "Tên đài không hợp lệ: " + String.join(", ", dsSai),
////                            "Các đài hợp lệ hôm nay: " + String.join(", ", dsDaiTrongNgay)
////                    ));
////                    return dto;
////                }
////
////                if (dsDaiNguoiChoi.size() < dsDaiTrongNgay.size()) {
////                    dto.setGhiChu("Bạn chỉ dò một phần đài hôm nay. Đã chọn: " + String.join(", ", dsDaiNguoiChoi));
////                }
////
////                boolean trung = false;
////                String giaiTrung = "";
////                String tenDaiTrung = "";
////                List<String> danhSachDaiHopLe = new ArrayList<>();
////
////                for (String tenDaiChuan : dsDaiNguoiChoi) {
////                    if (mienChuan.equals("MIENBAC")) {
////                        var ketQua = bacRepo.findAllByNgay(ngay).stream()
////                                .filter(kq -> "G7".equalsIgnoreCase(kq.getGiai()) && chuanHoa(kq.getTenDai()).equals(tenDaiChuan))
////                                .toList();
////                        for (var kq : ketQua) {
////                            danhSachDaiHopLe.add(kq.getTenDai());
////                            if (soChuan.equals(chuanHoa(kq.getSoTrung()))) {
////                                trung = true;
////                                giaiTrung = "Giải 7";
////                                tenDaiTrung = kq.getTenDai();
////                                break;
////                            }
////                        }
////                    } else if (mienChuan.equals("MIENTRUNG")) {
////                        var ketQua = trungRepo.findAllByNgay(ngay).stream()
////                                .filter(kq -> "G8".equalsIgnoreCase(kq.getGiai()) && chuanHoa(kq.getTenDai()).equals(tenDaiChuan))
////                                .toList();
////                        for (var kq : ketQua) {
////                            danhSachDaiHopLe.add(kq.getTenDai());
////                            if (soChuan.equals(chuanHoa(kq.getSoTrung()))) {
////                                trung = true;
////                                giaiTrung = "Giải 8";
////                                tenDaiTrung = kq.getTenDai();
////                                break;
////                            }
////                        }
////                    } else if (mienChuan.equals("MIENNAM")) {
////                        var ketQua = namRepo.findAllByNgay(ngay).stream()
////                                .filter(kq -> "G8".equalsIgnoreCase(kq.getGiai()) && chuanHoa(kq.getTenDai()).equals(tenDaiChuan))
////                                .toList();
////                        for (var kq : ketQua) {
////                            danhSachDaiHopLe.add(kq.getTenDai());
////                            if (soChuan.equals(chuanHoa(kq.getSoTrung()))) {
////                                trung = true;
////                                giaiTrung = "Giải 8";
////                                tenDaiTrung = kq.getTenDai();
////                                break;
////                            }
////                        }
////                    }
////                }
////
////                dto.setDanhSachDai(danhSachDaiHopLe);
////                dto.setTrung(trung);
////                if (trung) {
////                    dto.setGiaiTrung(giaiTrung);
////                    dto.setTenDai(tenDaiTrung);
////                    dto.setCachTrung("Trúng ĐẦU – Đài " + tenDaiTrung);
////                    try {
////                        dto.setTienTrung(Double.parseDouble(tienDanh) * 1000);
////                    } catch (Exception e) {
////                        dto.setTienTrung(0.0);
////                        dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ"));
////                    }
////                } else {
////                    dto.setTienTrung(0.0);
////                    dto.setSaiLyDo(List.of("Không trúng ở các đài đã chọn"));
////                }
////
////                return dto;
////            }
////
////            private String chuanHoa(String input) {
////                if (input == null) return "";
////                return Normalizer.normalize(input.trim().toUpperCase(), Normalizer.Form.NFD)
////                        .replaceAll("\\p{M}", "") // bỏ dấu tiếng Việt
////                        .replaceAll("\\s+", " ")  // gọn khoảng trắng
////                        .trim();
////            }
////
////        private List<String> layDanhSachDaiTrongNgay(String mien, LocalDate ngay) {
////            LichQuayXoSo lich = lichQuayXoSoService.traCuuTheoNgay(ngay);
////            Map<String, List<String>> danhSachDai = lich.getDanhSachDai(); // đúng tên field
////
////            String mienChuan = chuanHoa(mien);
////
////            return danhSachDai.entrySet().stream()
////                    .filter(entry -> chuanHoa(entry.getKey()).equals(mienChuan))
////                    .flatMap(entry -> entry.getValue().stream())
////                    .map(this::chuanHoa)
////                    .distinct()
////                    .toList();
////        }
////
////
////        private String chuyenNgaySangThu(LocalDate ngay) {
////                // Tuỳ bạn viết logic chuyển ngày sang thứ
////                return ngay.getDayOfWeek().toString();
////            }
////        }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////        private List<String> laySoTrungTheoGiaiVaDai(LocalDate ngay, String tenGiai, String tenDaiChuan, String mienChuan) {
////            return switch (mienChuan) {
////                case "MIỀN BẮC" -> bacRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> tenGiai.equalsIgnoreCase(kq.getGiai()) &&
////                                chuanHoa(kq.getTenDai()).equals(tenDaiChuan))
////                        .map(kq -> kq.getSoTrung())
////                        .toList();
////
////                case "MIỀN TRUNG" -> trungRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> tenGiai.equalsIgnoreCase(kq.getGiai()) &&
////                                chuanHoa(kq.getTenDai()).equals(tenDaiChuan))
////                        .map(kq -> kq.getSoTrung())
////                        .toList();
////
////                case "MIỀN NAM" -> namRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> tenGiai.equalsIgnoreCase(kq.getGiai()) &&
////                                chuanHoa(kq.getTenDai()).equals(tenDaiChuan))
////                        .map(kq -> kq.getSoTrung())
////                        .toList();
////
////                default -> List.of();
////            };
////
//
//package com.example.doxoso.service;
//
//import com.example.doxoso.model.DoiChieuKetQuaDto;
//import com.example.doxoso.repository.KetQuaMienBacRepository;
//import com.example.doxoso.repository.KetQuaMienNamRepository;
//import com.example.doxoso.repository.KetQuaMienTrungRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.text.Normalizer;
//import java.time.DayOfWeek;
//import java.time.LocalDate;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.stream.Collectors;
//
//@Service
//public class DauService {
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
//    public DoiChieuKetQuaDto xuLyDau(String soDanh, String mien, LocalDate ngay, String tienDanh, String tenDai) {
//        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
//        dto.setSoDanh(soDanh);
//        dto.setTenDai(tenDai.trim());
//        dto.setMien(mien);
//        dto.setNgay(ngay);
//        dto.setThu(chuyenNgaySangThu(ngay));
//        dto.setCachDanh("ĐẦU");
//        dto.setCachTrung("ĐẦU");
//        dto.setTienDanh(tienDanh);
//
//        // Validate cơ bản
//        if (soDanh == null || tienDanh == null) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Thiếu thông tin bắt buộc (số đánh hoặc tiền đánh)"));
//            return dto;
//        }
//        if (!soDanh.matches("\\d{2}")) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải là 2 chữ số)"));
//            return dto;
//        }
//        if (tenDai == null || tenDai.isBlank()) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Bạn phải nhập tên đài muốn dò"));
//            return dto;
//        }
//
//        List<String> dsDaiTrongNgay = layDanhSachDaiCoDauTrongNgay(mien, ngay);
//        if (dsDaiTrongNgay.isEmpty()) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Không có đài mở thưởng trong ngày " + ngay));
//            return dto;
//        }
//
//        // Xác định danh sách đài người chơi chọn
//        List<String> dsDaiNguoiChoi;
//        String tenDaiNormalized = tenDai.trim().toUpperCase();
//        if (tenDaiNormalized.matches("\\d+\\s*ĐÀI")) {
//            int soDaiNguoiNhap = Integer.parseInt(tenDaiNormalized.split("\\s+")[0]);
//            if (soDaiNguoiNhap == dsDaiTrongNgay.size()) {
//                dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
//            } else {
//                dto.setTrung(false);
//                dto.setTienTrung(0.0);
//                dto.setSaiLyDo(List.of("Số đài nhập (" + soDaiNguoiNhap + ") không khớp với số đài mở thưởng trong ngày (" + dsDaiTrongNgay.size() + ")"));
//                dto.setDanhSachDai(dsDaiTrongNgay);
//                return dto;
//            }
//        } else {
//            dsDaiNguoiChoi = Arrays.stream(tenDai.split(","))
//                    .map(String::trim)
//                    .filter(s -> !s.isEmpty())
//                    .collect(Collectors.toList());
//
//            List<String> dsSai = dsDaiNguoiChoi.stream()
//                    .filter(dai -> dsDaiTrongNgay.stream().noneMatch(d -> d.equalsIgnoreCase(dai)))
//                    .collect(Collectors.toList());
//
//            if (!dsSai.isEmpty()) {
//                dto.setTrung(false);
//                dto.setTienTrung(0.0);
//                dto.setSaiLyDo(List.of("Tên đài không hợp lệ: " + String.join(", ", dsSai)));
//                dto.setDanhSachDai(dsDaiTrongNgay);
//                return dto;
//            }
//        }
//
//        // Chuyển tiền đánh
//        double tienDanhDouble;
//        try {
//            tienDanhDouble = Double.parseDouble(tienDanh);
//        } catch (NumberFormatException e) {
//            dto.setTrung(false);
//            dto.setTienTrung(0.0);
//            dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ"));
//            return dto;
//        }
//
//        double tienDanhMoiDai = tienDanhDouble / dsDaiNguoiChoi.size();
//        AtomicBoolean daTrung = new AtomicBoolean(false);
//        double[] tongTienTrung = {0.0};
//
//        // Xử lý từng đài
//        List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTungDaiList = dsDaiNguoiChoi.stream().map(daiNguoiChoi -> {
//            DoiChieuKetQuaDto.KetQuaTheoDai ketQuaDai = new DoiChieuKetQuaDto.KetQuaTheoDai();
//            ketQuaDai.setTenDai(daiNguoiChoi);
//            ketQuaDai.setMien(mien);
//
//            List<?> ketQuaDaiHienTai = getKetQuaTheoDaiVaNgay(mien, daiNguoiChoi, ngay);
//
//            List<String> giaiTrung = ketQuaDaiHienTai.stream()
//                    .filter(kq -> {
//                        String soTrung = (String) getField(kq, "getSoTrung");
//                        return soTrung != null && soTrung.length() >= 2 &&
//                                soTrung.startsWith(soDanh);
//                    })
//                    .map(kq -> (String) getField(kq, "getGiai"))
//                    .distinct()
//                    .collect(Collectors.toList());
//
//            if (!giaiTrung.isEmpty()) {
//                ketQuaDai.setTrung(true);
//                ketQuaDai.setGiaiTrung(giaiTrung);
//                ketQuaDai.setSoTrung(soDanh);
//                ketQuaDai.setSoLanTrung(giaiTrung.size());
////tính tiền
//                double tienTrungDai = tinhTienService.tinhTienDau(true, mien, tienDanhMoiDai) * ketQuaDai.getSoLanTrung();
//
//
//
//
//                ketQuaDai.setTienTrung(tienTrungDai);
//
//                tongTienTrung[0] += tienTrungDai;
//                daTrung.set(true);
//            } else {
//                ketQuaDai.setTrung(false);
//                ketQuaDai.setTienTrung(0.0);
//                ketQuaDai.setLyDo("Không có số trúng " + soDanh);
//                ketQuaDai.setSoLanTrung(0);
//            }
//
//            return ketQuaDai;
//        }).collect(Collectors.toList());
//
//        // Set dữ liệu cuối cùng
//        dto.setTrung(daTrung.get());
//        dto.setTienTrung(tongTienTrung[0]);
//        dto.setDanhSachDai(dsDaiNguoiChoi);
//        dto.setKetQuaTungDai(ketQuaTungDaiList);
//
//        return dto;
//    }
//
//    private List<String> layDanhSachDaiCoDauTrongNgay(String mien, LocalDate ngay) {
//        switch (mien) {
//            case "MIỀN BẮC":
//                return bacRepo.findAllByNgay(ngay).stream()
//                        .map(kq -> kq.getTenDai().trim())
//                        .distinct()
//                        .toList();
//            case "MIỀN TRUNG":
//                return trungRepo.findAllByNgay(ngay).stream()
//                        .map(kq -> kq.getTenDai().trim())
//                        .distinct()
//                        .toList();
//            case "MIỀN NAM":
//                return namRepo.findAllByNgay(ngay).stream()
//                        .map(kq -> kq.getTenDai().trim())
//                        .distinct()
//                        .toList();
//            default:
//                return Collections.emptyList();
//        }
//    }
//
//
//    private List<?> getKetQuaTheoDaiVaNgay(String mien, String tenDai, LocalDate ngay) {
//        switch (mien) {
//            case "MIỀN BẮC":
//                return bacRepo.findAllByNgay(ngay).stream()
//                        .filter(kq -> chuanHoa(kq.getTenDai()).equals(chuanHoa(tenDai)))
//                        .toList();
//            case "MIỀN TRUNG":
//                return trungRepo.findAllByNgay(ngay).stream()
//                        .filter(kq -> chuanHoa(kq.getTenDai()).equals(chuanHoa(tenDai)))
//                        .toList();
//            case "MIỀN NAM":
//                return namRepo.findAllByNgay(ngay).stream()
//                        .filter(kq -> chuanHoa(kq.getTenDai()).equals(chuanHoa(tenDai)))
//                        .toList();
//            default:
//                return Collections.emptyList();
//        }
//    }
//
//
//    private String chuyenNgaySangThu(LocalDate ngay) {
//        DayOfWeek day = ngay.getDayOfWeek();
//        return switch (day) {
//            case MONDAY -> "Thứ Hai";
//            case TUESDAY -> "Thứ Ba";
//            case WEDNESDAY -> "Thứ Tư";
//            case THURSDAY -> "Thứ Năm";
//            case FRIDAY -> "Thứ Sáu";
//            case SATURDAY -> "Thứ Bảy";
//            case SUNDAY -> "Chủ Nhật";
//        };
//    }
//
//    private String chuanHoa(String input) {
//        if (input == null) return "";
//        return Normalizer.normalize(input, Normalizer.Form.NFD)
//                .replaceAll("đ", "d")
//                .replaceAll("Đ", "D")
//                .replaceAll("\\p{M}", "")
//                .toUpperCase()
//                .trim()
//                .replaceAll("\\s+", "");
//    }
//
//    private <T> T getField(Object obj, String methodName) {
//        try {
//            return (T) obj.getClass().getMethod(methodName).invoke(obj);
//        } catch (Exception e) {
//            throw new RuntimeException("Lỗi phản xạ khi lấy field: " + methodName, e);
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////    public DoiChieuKetQuaDto xuLyDau(String soDanh, String mien, LocalDate ngay, String tienDanh, String tenDai) {
////        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
////        dto.setSoDanh(soDanh);
////        dto.setMien(mien);
////        dto.setNgay(ngay);
////        dto.setThu(chuyenNgaySangThu(ngay));
////        dto.setCachDanh("DAU");
////        dto.setTienDanh(tienDanh);
////
////        // Validate số đánh
////        if (soDanh == null || !soDanh.matches("\\d{2}")) {
////            dto.setTrung(false);
////            dto.setTienTrung(0.0);
////            dto.setSaiLyDo(List.of("Số đánh không hợp lệ (phải là 2 chữ số)"));
////            return dto;
////        }
////
////        // Chuẩn hóa miền và tên đài
////        String mienChuan = chuanHoa(mien);
////        String soChuan = chuanHoa(soDanh);
////        String tenDaiRaw = tenDai == null ? "" : tenDai.trim().toUpperCase();
////
////        // Lấy kết quả tổng hợp theo miền và ngày
////        List<Object> ketQuaTongHop = new ArrayList<>();
////        switch (mienChuan) {
////            case "MIENBAC" -> ketQuaTongHop.addAll(bacRepo.findAllByNgay(ngay));
////            case "MIENTRUNG" -> ketQuaTongHop.addAll(trungRepo.findAllByNgay(ngay));
////            case "MIENNAM" -> ketQuaTongHop.addAll(namRepo.findAllByNgay(ngay));
////            default -> {
////                dto.setTrung(false);
////                dto.setTienTrung(0.0);
////                dto.setSaiLyDo(List.of("Miền không hợp lệ"));
////                return dto;
////            }
////        }
////
////        // Lấy danh sách đài mở trong ngày theo miền (chuẩn hóa tên đài)
////        Set<String> dsDaiTrongNgay = ketQuaTongHop.stream()
////                .map(kq -> (String) getField(kq, "getTenDai"))
////                .filter(Objects::nonNull)
////                .map(String::toUpperCase)
////                .map(String::trim)
////                .map(this::chuanHoa)
////                .collect(Collectors.toSet());
////
////        // Xử lý tên đài người chơi nhập
////        List<String> dsDaiNguoiChoi;
////        boolean isNhapDangXDAI = tenDaiRaw.matches("\\d\\s*ĐÀI"); // vd: "3 ĐÀI"
////
////        if (isNhapDangXDAI) {
////            int soDaiNguoiNhap = Integer.parseInt(tenDaiRaw.split("\\s")[0]);
////            int soDaiThucTe = dsDaiTrongNgay.size();
////
////            if (soDaiNguoiNhap == soDaiThucTe) {
////                dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
////            } else if (soDaiNguoiNhap < soDaiThucTe) {
////                dto.setTrung(false);
////                dto.setTienTrung(0.0);
////                dto.setSaiLyDo(List.of("Hôm nay có " + soDaiThucTe + " đài, bạn cần ghi rõ tên " + soDaiNguoiNhap + " đài muốn đánh!"));
////                return dto;
////            } else {
////                dto.setTrung(false);
////                dto.setTienTrung(0.0);
////                dto.setSaiLyDo(List.of("Hôm nay chỉ có " + soDaiThucTe + " đài, bạn không thể đánh " + soDaiNguoiNhap + " đài!"));
////                return dto;
////            }
////        } else {
////            // Tách danh sách đài người chơi nhập, chuẩn hóa từng đài
////            dsDaiNguoiChoi = Arrays.stream(tenDaiRaw.split("\\s*,\\s*"))
////                    .filter(s -> !s.isBlank())
////                    .map(this::chuanHoa)
////                    .toList();
////
////            if (dsDaiNguoiChoi.isEmpty()) {
////                if (dsDaiTrongNgay.size() == 1) {
////                    dsDaiNguoiChoi = new ArrayList<>(dsDaiTrongNgay);
////                } else {
////                    dto.setTrung(false);
////                    dto.setTienTrung(0.0);
////                    dto.setSaiLyDo(List.of("Bạn phải nhập tên đài muốn dò"));
////                    return dto;
////                }
////            }
////        }
////
////        // Kiểm tra tên đài nhập có hợp lệ
////        List<String> dsSai = dsDaiNguoiChoi.stream()
////                .filter(dai -> !dsDaiTrongNgay.contains(dai))
////                .toList();
////
////        if (!dsSai.isEmpty()) {
////            dto.setTrung(false);
////            dto.setTienTrung(0.0);
////            dto.setSaiLyDo(List.of(
////                    "Tên đài không hợp lệ: " + String.join(", ", dsSai),
////                    "Các đài hợp lệ hôm nay: " + String.join(", ", dsDaiTrongNgay)
////            ));
////            return dto;
////        }
////
////        // Dò số trên từng đài người chơi nhập
////        List<DoiChieuKetQuaDto.KetQuaTheoDai> danhSachKetQua = dsDaiNguoiChoi.stream().map(tenDaiChuan -> {
////            DoiChieuKetQuaDto.KetQuaTheoDai ketQuaDai = new DoiChieuKetQuaDto.KetQuaTheoDai();
////            ketQuaDai.setTenDai(tenDaiChuan);
////            ketQuaDai.setMien(mien);
////
////            List<String> giaiTrung = ketQuaTongHop.stream()
////                    .filter(kq -> tenDaiChuan.equalsIgnoreCase(chuanHoa((String) getField(kq, "getTenDai"))))
////                    .filter(kq -> {
////                        String soTrung = (String) getField(kq, "getSoTrung");
////                        if (soTrung != null && soTrung.length() >= 2) {
////                            return soChuan.equals(chuanHoa(soTrung));
////                        }
////                        return false;
////                    })
////                    .map(kq -> (String) getField(kq, "getGiai"))
////                    .distinct()
////                    .toList();
////
////            if (!giaiTrung.isEmpty()) {
////                ketQuaDai.setTrung(true);
////                ketQuaDai.setGiaiTrung(giaiTrung);
////                ketQuaDai.setSoTrung(soChuan);
////                ketQuaDai.setSoLanTrung(giaiTrung.size());
////                // Tính tiền trúng từng đài nếu cần, ví dụ tạm set 0 ở đây (hoặc gọi tính tiền theo từng đài)
////                ketQuaDai.setTienTrung(0);
////            } else {
////                ketQuaDai.setTrung(false);
////                ketQuaDai.setLyDo("Không có số trúng " + soChuan);
////                ketQuaDai.setSoLanTrung(0);
////                ketQuaDai.setTienTrung(0);
////            }
////
////            return ketQuaDai;
////        }).toList();
////
////        // Tổng hợp trạng thái trúng
////        boolean trung = danhSachKetQua.stream().anyMatch(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung);
////        dto.setTrung(trung);
////        dto.setKetQuaTungDai(danhSachKetQua);
////        dto.setDanhSachDai(dsDaiNguoiChoi);
////
////        // Tính tổng tiền trúng (gọi dịch vụ tính tiền)
////        if (trung) {
////            try {
////                double tienDanhDouble = Double.parseDouble(tienDanh);
////                double tongTienTrung = tinhTienService.tinhTienDau(true, mien, tienDanhDouble);
////                dto.setTienTrung(tongTienTrung);
////                // Bạn có thể muốn phân bổ tiền trúng chi tiết cho từng đài trong danhSachKetQua ở đây
////            } catch (NumberFormatException e) {
////                dto.setTienTrung(0.0);
////                dto.setSaiLyDo(List.of("Tiền đánh không hợp lệ"));
////            }
////        } else {
////            dto.setTienTrung(0.0);
////            dto.setSaiLyDo(List.of("Không trúng ở các đài đã chọn"));
////        }
////
////        return dto;
////    }
////
////
////    private List<?> getKetQuaTheoDaiVaNgay(String mien, String tenDai, LocalDate ngay) {
////        switch (mien) {
////            case "MIỀN BẮC":
////                return bacRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> chuanHoa(kq.getTenDai()).equals(chuanHoa(tenDai)))
////                        .toList();
////            case "MIỀN TRUNG":
////                return trungRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> chuanHoa(kq.getTenDai()).equals(chuanHoa(tenDai)))
////                        .toList();
////            case "MIỀN NAM":
////                return namRepo.findAllByNgay(ngay).stream()
////                        .filter(kq -> chuanHoa(kq.getTenDai()).equals(chuanHoa(tenDai)))
////                        .toList();
////            default:
////                return Collections.emptyList();
////        }
////    }
////
////
////    private List<String> layDanhSachDaiCoDauTrongNgay(String mien, LocalDate ngay) {
////        switch (mien) {
////            case "MIỀN BẮC":
////                return bacRepo.findAllByNgay(ngay).stream()
////                        .map(kq -> kq.getTenDai().trim())
////                        .distinct()
////                        .toList();
////            case "MIỀN TRUNG":
////                return trungRepo.findAllByNgay(ngay).stream()
////                        .map(kq -> kq.getTenDai().trim())
////                        .distinct()
////                        .toList();
////            case "MIỀN NAM":
////                return namRepo.findAllByNgay(ngay).stream()
////                        .map(kq -> kq.getTenDai().trim())
////                        .distinct()
////                        .toList();
////            default:
////                return Collections.emptyList();
////        }
////    }
////
////    private String chuyenNgaySangThu(LocalDate ngay) {
////            DayOfWeek day = ngay.getDayOfWeek();
////            return switch (day) {
////                case MONDAY -> "Thứ Hai";
////                case TUESDAY -> "Thứ Ba";
////                case WEDNESDAY -> "Thứ Tư";
////                case THURSDAY -> "Thứ Năm";
////                case FRIDAY -> "Thứ Sáu";
////                case SATURDAY -> "Thứ Bảy";
////                case SUNDAY -> "Chủ Nhật";
////            };
////        }
////
////            private String chuanHoa(String input) {
////                if (input == null) return "";
////                return Normalizer.normalize(input, Normalizer.Form.NFD)
////                        .replaceAll("đ", "d")
////                        .replaceAll("Đ", "D")
////                        .replaceAll("\\p{M}", "")
////                        .toUpperCase()
////                        .trim()
////                        .replaceAll("\\s+", "");
////            }
////    private <T> T getField(Object obj, String methodName) {
////        try {
////            return (T) obj.getClass().getMethod(methodName).invoke(obj);
////        } catch (Exception e) {
////            throw new RuntimeException("Lỗi phản xạ khi lấy field: " + methodName);
////        }
////    }
////
////}
