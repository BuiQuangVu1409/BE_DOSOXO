//////package com.example.doxoso.service;
//////
//////import com.example.doxoso.model.KetQuaMienBac;
//////import com.example.doxoso.repository.KetQuaMienBacRepository;
//////import com.example.doxoso.repository.KetQuaMienNamRepository;
//////import com.example.doxoso.repository.KetQuaMienTrungRepository;
//////import org.springframework.beans.factory.annotation.Autowired;
//////import org.springframework.stereotype.Service;
//////
//////import java.lang.reflect.Method;
//////import java.text.Normalizer;
//////import java.time.LocalDate;
//////import java.util.ArrayList;
//////import java.util.List;
//////import java.util.Objects;
//////import java.util.regex.Pattern;
//////
//////@Service
//////public class TinhTienService implements ITinhTienService {
//////    @Autowired
//////    KetQuaMienBacRepository bacRepo;
//////    @Autowired
//////    KetQuaMienNamRepository namRepo;
//////    @Autowired
//////    KetQuaMienTrungRepository trungRepo;
//////    public double tinhTienTrung(String cachDanh, String tienDanh, String mien) {
//////        String loai = removeDiacritics(cachDanh).toUpperCase().replaceAll("\\s+", "");
//////
//////        if (loai.equals("3CHAN")) return 0; // đã xử lý riêng
//////
//////        double tienDanhDouble;
//////        try {
//////            tienDanhDouble = Double.parseDouble(tienDanh);
//////        } catch (NumberFormatException e) {
//////            throw new IllegalArgumentException("Giá trị tiền không hợp lệ: " + tienDanh, e);
//////        }
//////
//////        return switch (loai) {
//////            //tính tiền xuyên
//////            case "XUYEN2" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 10;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 15;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 15;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////            case "XUYEN3" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 40;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 60;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 60;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////            case "XUYEN4" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 100;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 120;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 120;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////            case "XUYEN5" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 200;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 250;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 250;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////
//////
//////            // tính tiền 2 chân
//////            case "2CHAN" -> {
//////                String m = removeDiacritics(mien).toUpperCase().trim();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 70 / 27 ;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 70 / 18;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 70/ 18 ;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////
////////            case "DAUMIENBAC" -> tienDanhDouble * 70 / 4 ;
////////            case "DAUMIENTRUNG", "DAUMIENNAM" -> tienDanhDouble * 70;
////////            case "DUOIMIENBAC" -> tienDanhDouble * 70;
////////            case "DUOIMIENTRUNG", "DUOIMIENNAM" -> tienDanhDouble * 70;
////////            case "DAUDUOIMIENBAC" -> tienDanhDouble * 70 / 5;
////////            case "DAUDUOIMIENTRUNG", "DAUDUOIMIENNAM" -> tienDanhDouble * 70 / 2 ;
//////
//////            case "DAU" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 70 / 4;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 70;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 70;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////            // ĐUÔI
//////            case "DUOI" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 70;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 70;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 70;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////
//////            // ĐẦU ĐUÔI
//////            case "DAUDUOI" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 70 / 5;
//////                } else if (m.contains("TRUNG")) {
//////                    yield tienDanhDouble * 70 / 2;
//////                } else if (m.contains("NAM")) {
//////                    yield tienDanhDouble * 70 / 2;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////            // LỚN , nhỏ
//////            case "LON", "NHO" -> {
//////                String m = removeDiacritics(mien).toUpperCase();
//////                if (m.contains("BAC")) {
//////                    yield tienDanhDouble * 1.95;
//////                } else if (m.contains("TRUNG") || m.contains("NAM")) {
//////                    yield tienDanhDouble * 1.95;
//////                } else {
//////                    yield 0;
//////                }
//////            }
//////
//////            default -> 0;
//////        };
//////    }
//////    public double tinhTienDauDuoi(boolean trungDau, boolean trungDuoi, String mien, double tienDanh) {
//////        String m = removeDiacritics(mien).toUpperCase();
//////
//////        if (trungDau && trungDuoi) {
//////            // Trúng cả ĐẦU và ĐUÔI → dùng công thức như bạn đã có
//////            if (m.contains("BAC")) return tienDanh * 70 / 5;
//////            if (m.contains("TRUNG") || m.contains("NAM")) return tienDanh * 70 / 2;
//////        } else if (trungDau) {
//////            // Trúng ĐẦU (cách đánh ĐẦU ĐUÔI)
//////            return tienDanh * 500; // hệ số riêng cho ĐẦU trong DAUDUOI
//////        } else if (trungDuoi) {
//////            // Trúng ĐUÔI (cách đánh ĐẦU ĐUÔI)
//////            return tienDanh * 35;  // hệ số riêng cho ĐUÔI trong DAUDUOI
//////        }
//////
//////        return 0;
//////    }
//////
//////
//////
//////    private String removeDiacritics(String input) {
//////        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
//////        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
//////                .matcher(normalized)
//////                .replaceAll("");
//////    }
//////
//////}
////////    private double tinhTienLon(double tien, String mien) {
////////        String m = removeDiacritics(mien).toUpperCase();
////////        if (m.contains("BAC")) return tien * 1.8;
////////        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 2;
////////        return 0;
////////    }
////////
////////    private double tinhTienNho(double tien, String mien) {
////////        return tinhTienLon(tien, mien); // vì logic giống nhau
////////    }
////////case "LON" -> tinhTienLon(tienDanhDouble, mien);
////////        case "NHO" -> tinhTienNho(tienDanhDouble, mien);
////
////
////
////
////
////
////
////
////
////
////
////
////
////package com.example.doxoso.service;
////
////import com.example.doxoso.model.DoiChieuKetQuaDto;
////import com.example.doxoso.repository.KetQuaMienBacRepository;
////import com.example.doxoso.repository.KetQuaMienNamRepository;
////import com.example.doxoso.repository.KetQuaMienTrungRepository;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.stereotype.Service;
////
////import java.text.Normalizer;
////import java.util.List;
////import java.util.regex.Pattern;
////
////@Service
////public class TinhTienService implements ITinhTienService {
////    @Autowired
////    KetQuaMienBacRepository bacRepo;
////    @Autowired
////    KetQuaMienNamRepository namRepo;
////    @Autowired
////    KetQuaMienTrungRepository trungRepo;
////
////    public double tinhTienTrung(String cachDanh, String tienDanh, String mien) {
////        String loai = removeDiacritics(cachDanh).toUpperCase().replaceAll("\\s+", "");
////        double tienDanhDouble;
////
////        if (loai.equals("3CHAN")) return 0; // đã xử lý riêng
////
////        try {
////            tienDanhDouble = Double.parseDouble(tienDanh);
////        } catch (NumberFormatException e) {
////            throw new IllegalArgumentException("Giá trị tiền không hợp lệ: " + tienDanh, e);
////        }
////
////        return switch (loai) {
////            case "XUYEN2" -> getHeSoXuyen(2, tienDanhDouble, mien);
////            case "XUYEN3" -> getHeSoXuyen(3, tienDanhDouble, mien);
////            case "XUYEN4" -> getHeSoXuyen(4, tienDanhDouble, mien);
////            case "XUYEN5" -> getHeSoXuyen(5, tienDanhDouble, mien);
////            case "2CHAN"   -> getHeSo2Chan(tienDanhDouble, mien);
////            case "DAU"     -> getHeSoDau(tienDanhDouble, mien);
////            case "DUOI"    -> getHeSoDuoi(tienDanhDouble, mien);
////            case "DAUDUOI" -> getHeSoDauDuoiCaCap(tienDanhDouble, mien);
////            case "LON", "NHO" -> getHeSoLonNho(tienDanhDouble, mien);
////            default -> 0;
////        };
////    }
////
////    public double tinhTienDauDuoi(boolean trungDau, boolean trungDuoi,
////                                  String mienTrungDau, String mienTrungDuoi,
////                                  double tienDanh) {
////        double tien = 0;
////
////        if (trungDau && trungDuoi) {
////            // Nếu cả hai cùng trúng và cùng miền → xử lý gộp
////            if (mienTrungDau.equals(mienTrungDuoi)) {
////                tien += getHeSoDauDuoiCaCap(tienDanh, mienTrungDau);
////            } else {
////                // Trúng cả 2 nhưng khác miền → tính riêng
////                tien += tinhTienDau(trungDau, mienTrungDau, tienDanh / 2);
////                tien += tinhTienDuoi(trungDuoi, mienTrungDuoi, tienDanh / 2);
////            }
////        } else if (trungDau) {
////            tien += tinhTienDau(true, mienTrungDau, tienDanh);
////        } else if (trungDuoi) {
////            tien += tinhTienDuoi(true, mienTrungDuoi, tienDanh);
////        }
////
////        return tien;
////    }
////
////
////    // === HÀM PHỤ ===
////    private double tinhTienDau(boolean trungDau, String mien, double tienDanh) {
////        if (!trungDau) return 0;
////        switch (mien) {
////            case "MIỀN BẮC":
////                return tienDanh * 70/4;
////            case "MIỀN TRUNG":
////                return tienDanh * 70;
////            case "MIỀN NAM":
////                return tienDanh * 70;
////            default:
////                return 0;
////        }
////    }
////
////    private double tinhTienDuoi(boolean trungDuoi, String mien, double tienDanh) {
////        if (!trungDuoi) return 0;
////        switch (mien) {
////            case "MIỀN BẮC":
////                return tienDanh * 70;
////            case "MIỀN TRUNG":
////                return tienDanh * 70;
////            case "MIỀN NAM":
////                return tienDanh * 70;
////            default:
////                return 0;
////        }
////    }
////    private double[] tachTienDanhBaChan(String tienChuoi) {
////        double[] tien = new double[] {0.0, 0.0, 0.0}; // [baoLo, thuong, dacBiet]
////        if (tienChuoi == null || tienChuoi.trim().isEmpty()) return tien;
////
////        String[] parts = tienChuoi.trim().split("-");
////        for (int i = 0; i < Math.min(3, parts.length); i++) {
////            try {
////                tien[i] = Double.parseDouble(parts[i].trim());
////            } catch (NumberFormatException ignored) {}
////        }
////
////        return tien;
////    }
////
////    private double getHeSoBaoLo(String mien, double tien) {
////        if (mien.contains("BAC")) return tien * 600 / 23;
////        if (mien.contains("TRUNG") || mien.contains("NAM")) return tien * 600 / 17;
////        return 0;
////    }
////
////    private double getHeSoThuong(String mien, double tien) {
////        if (mien.contains("BAC")) return tien * 600 / 10;
////        if (mien.contains("TRUNG") || mien.contains("NAM")) return tien * 100;
////        return 0;
////    }
////
////    private boolean isLoThuong(String mien, String giai) {
////        return switch (mien) {
////            case "BAC" -> giai.equals("G5") || giai.equals("G6") || giai.equals("ĐẶC BIỆT");
////            case "TRUNG", "NAM" -> giai.equals("G5") || giai.equals("G6") || giai.equals("G7") || giai.equals("ĐẶC BIỆT");
////            default -> false;
////        };
////    }
////
////    public double tinhTienBaChan(String tienChuoi, String mien, List<DoiChieuKetQuaDto.KetQuaTheoDai> ketQuaTheoDai) {
////        double[] tien = tachTienDanhBaChan(tienChuoi); // [baoLo, thuong, dacBiet]
////        double tienBaoLo = tien[0];
////        double tienThuong = tien[1];
////        double tienDacBiet = tien[2];
////
////        double tongTien = 0;
////
////        for (DoiChieuKetQuaDto.KetQuaTheoDai dai : ketQuaTheoDai) {
////            if (!dai.isTrung()) continue;
////
////            String tenMien = removeDiacritics(dai.getMien()).toUpperCase();
////
////            for (String giai : dai.getGiai()) {
////                String g = giai.trim().toUpperCase();
////
////                double tienBL = 0, tienT = 0, tienDB = 0;
////
////                if (g.equals("ĐẶC BIỆT")) {
////                    tienBL = getHeSoBaoLo(tenMien, tienBaoLo);
////                    tienT = getHeSoThuong(tenMien, tienThuong);
////                    tienDB = tienDacBiet * 600;
////                } else if (isLoThuong(tenMien, g)) {
////                    tienBL = getHeSoBaoLo(tenMien, tienBaoLo);
////                    tienT = getHeSoThuong(tenMien, tienThuong);
////                } else {
////                    tienBL = getHeSoBaoLo(tenMien, tienBaoLo);
////                }
////
////                tongTien += tienBL + tienT + tienDB;
////            }
////        }
////
////        return tongTien;
////    }
////
////
////
////    private double getHeSoXuyen(int soXuyen, double tien, String mien) {
////        String m = removeDiacritics(mien).toUpperCase();
////        int heSo = 0;
////
////        if (soXuyen == 2) {
////            if (m.contains("BAC")) heSo = 10;
////            else if (m.contains("TRUNG")) heSo = 15;
////            else if (m.contains("NAM")) heSo = 15;
////        } else if (soXuyen == 3) {
////            if (m.contains("BAC")) heSo = 40;
////            else if (m.contains("TRUNG")) heSo = 60;
////            else if (m.contains("NAM")) heSo = 60;
////        } else if (soXuyen == 4) {
////            if (m.contains("BAC")) heSo = 100;
////            else if (m.contains("TRUNG")) heSo = 120;
////            else if (m.contains("NAM")) heSo = 120;
////        } else if (soXuyen == 5) {
////            if (m.contains("BAC")) heSo = 200;
////            else if (m.contains("TRUNG")) heSo = 250;
////            else if (m.contains("NAM")) heSo = 250;
////        }
////
////        return tien * heSo;
////    }
////
////    private double getHeSo2Chan(double tien, String mien) {
////        String m = removeDiacritics(mien).toUpperCase();
////        if (m.contains("BAC")) return tien * 70 / 27;
////        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70 / 18;
////        return 0;
////    }
////
////    private double getHeSoDau(double tien, String mien) {
////        String m = removeDiacritics(mien).toUpperCase();
////        if (m.contains("BAC")) return tien * 70 / 4;
////        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70;
////        return 0;
////    }
////
////    private double getHeSoDuoi(double tien, String mien) {
////        String m = removeDiacritics(mien).toUpperCase();
////        return m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM") ? tien * 70 : 0;
////    }
////
////    private double getHeSoDauDuoiCaCap(double tien, String mien) {
////        String m = removeDiacritics(mien).toUpperCase();
////        if (m.contains("BAC")) return tien * 70 / 5;
////        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70 / 2;
////        return 0;
////    }
////
////    private double getHeSoLonNho(double tien, String mien) {
////        String m = removeDiacritics(mien).toUpperCase();
////        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) return tien * 1.95;
////        return 0;
////    }
////    public double tinhTongTien2Chan(String mien, double tienDanh, int soLanTrung) {
////        if (soLanTrung <= 0) return 0;
////        double tienMotLan = getHeSo2Chan(tienDanh, mien);
////        return tienMotLan * soLanTrung;
////    }
////
////
////    private String removeDiacritics(String input) {
////        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
////        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
////                .matcher(normalized)
////                .replaceAll("");
////    }
////}
//package com.example.doxoso.service;
//
//
//import com.example.doxoso.model.Player;
//import com.example.doxoso.repository.KetQuaMienBacRepository;
//import com.example.doxoso.repository.KetQuaMienNamRepository;
//import com.example.doxoso.repository.KetQuaMienTrungRepository;
//import com.example.doxoso.repository.PlayerRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.text.Normalizer;
//import java.util.List;
//import java.util.regex.Pattern;
//
//@Service
//public class TinhTienService  {
//    @Autowired
//    KetQuaMienBacRepository bacRepo;
//    @Autowired
//    KetQuaMienNamRepository namRepo;
//    @Autowired
//    KetQuaMienTrungRepository trungRepo;
//    @Autowired
//    PlayerRepository playerRepository;
//
//    //====TÍNH TIỀN 2 CHÂN ============//
//
//    public double tinhTongTien2Chan(String mien, double tienDanh, int soLanTrung) {
//        if (soLanTrung <= 0) return 0;
//        double tienMotLan = getHeSo2Chan(tienDanh, mien);
//        return tienMotLan * soLanTrung;
//    }
//
//    private double getHeSo2Chan(double tien, String mien) {
//        String m = removeDiacritics(mien).toUpperCase();
//        if (m.contains("BAC")) return tien * 70 / 27;
//        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70 / 18;
//        return 0;
//    }
//
//
//
//
//    //========== TÍNH TIỀN ĐẦU ĐUÔI ==================
//    public double tinhTienDauDuoi(boolean trungDau, boolean trungDuoi,
//                                         String mienTrungDau, String mienTrungDuoi,
//                                         double tienDanhCuaDai) {
//        double tien = 0;
//
//        if (trungDau && trungDuoi) {
//            double tienDau = tienDanhCuaDai / 2;
//            double tienDuoi = tienDanhCuaDai / 2;
//
//            tien += tinhTienDau(true, mienTrungDau, tienDau);
//            tien += tinhTienDuoi(true, mienTrungDuoi, tienDuoi);
//
//        } else if (trungDau) {
//            tien = tinhTienDau(true, mienTrungDau, tienDanhCuaDai);
//
//        } else if (trungDuoi) {
//            tien = tinhTienDuoi(true, mienTrungDuoi, tienDanhCuaDai);
//
//        } else {
//            tien = 0;
//        }
//
//        return tien;
//    }
//
//
//
//
//
//    // === TÍNH TIỀN ĐẦU ===
//    double tinhTienDau(boolean trungDau, String mien, double tienDanh) {
//        if (!trungDau) return 0;
//        switch (mien) {
//            case "MIỀN BẮC":
//                return tienDanh * 70/4;
//            case "MIỀN TRUNG":
//                return tienDanh * 70;
//            case "MIỀN NAM":
//                return tienDanh * 70;
//            default:
//                return 0;
//        }
//    }
//    // === TÍNH TIỀN ĐUÔI ===
//    double tinhTienDuoi(boolean trungDuoi, String mien, double tienDanh) {
//        if (!trungDuoi) return 0;
//        switch (mien) {
//            case "MIỀN BẮC":
//                return tienDanh * 70;
//            case "MIỀN TRUNG":
//                return tienDanh * 70;
//            case "MIỀN NAM":
//                return tienDanh * 70;
//            default:
//                return 0;
//        }
//    }
//
//    // === TÍNH TIỀN 3 CHÂN ===
//    public double[] tinhTien3Chan(String tienChuoi, String mien, List<String> giaiTrung) {
//        double[] tien = tachTienDanhBaChan(tienChuoi);
//        double tienBaoLo = tien[0];
//        double tienThuong = tien[1];
//        double tienDacBiet = tien[2];
//
//        double tongTien = 0;
//        double tongBaoLo = 0;
//        double tongThuong = 0;
//        double tongDacBiet = 0;
//
//        for (String giai : giaiTrung) {
//            String g = chuanHoaTenGiai(giai);
//
//            if (mien.contains("BẮC")) {
//                // ==================== MIỀN BẮC ====================
//                if (g.equals("ĐẶC BIỆT")) {
//                    double tienDB = tienDacBiet * 600;
//                    double tienBL = tienBaoLo * 600 / 23;
//                    double tienT = tienThuong * 600 / 10;
//
//                    tongDacBiet += tienDB;
//                    tongBaoLo += tienBL;
//                    tongThuong += tienT;
//                    tongTien += tienDB + tienBL + tienT;
//
//                } else if (isLoThuong(mien, g)) {
//                    double tienBL = tienBaoLo * 600 / 23;
//                    double tienT = tienThuong * 600 / 10;
//
//                    tongBaoLo += tienBL;
//                    tongThuong += tienT;
//                    tongTien += tienBL + tienT;
//
//                } else {
//                    double tienBL = tienBaoLo * 600 / 23;
//
//                    tongBaoLo += tienBL;
//                    tongTien += tienBL;
//                }
//
//            } else if (mien.contains("TRUNG")) {
//                // ==================== MIỀN TRUNG ====================
//                if (g.equals("ĐẶC BIỆT")) {
//                    double tienDB = tienDacBiet * 600;
//                    double tienBL = tienBaoLo * 600 / 17;
//                    double tienT = tienThuong * 100;
//
//                    tongDacBiet += tienDB;
//                    tongBaoLo += tienBL;
//                    tongThuong += tienT;
//                    tongTien += tienDB + tienBL + tienT;
//
//                } else if (isLoThuong(mien, g)) {
//                    double tienBL = tienBaoLo * 600 / 17;
//                    double tienT = tienThuong * 100;
//
//                    tongBaoLo += tienBL;
//                    tongThuong += tienT;
//                    tongTien += tienBL + tienT;
//
//                } else {
//                    double tienBL = tienBaoLo * 600 / 17;
//
//                    tongBaoLo += tienBL;
//                    tongTien += tienBL;
//                }
//
//            } else if (mien.contains("NAM")) {
//                // ==================== MIỀN NAM ====================
//                if (g.equals("ĐẶC BIỆT")) {
//                    double tienDB = tienDacBiet * 600;
//                    double tienBL = tienBaoLo * 600 / 17;
//                    double tienT = tienThuong * 100;
//
//                    tongDacBiet += tienDB;
//                    tongBaoLo += tienBL;
//                    tongThuong += tienT;
//                    tongTien += tienDB + tienBL + tienT;
//
//                } else if (isLoThuong(mien, g)) {
//                    double tienBL = tienBaoLo * 600 / 17;
//                    double tienT = tienThuong * 100;
//
//                    tongBaoLo += tienBL;
//                    tongThuong += tienT;
//                    tongTien += tienBL + tienT;
//
//                } else {
//                    double tienBL = tienBaoLo * 600 / 17;
//
//                    tongBaoLo += tienBL;
//                    tongTien += tienBL;
//                }
//            }
//        }
//
//        return new double[]{
//                tongTien, tongBaoLo, tongThuong, tongDacBiet
//        };
//    }
//
//
//    //    // Xác định có phải lô thượng hay không (tùy miền)
//    public boolean isLoThuong(String mien, String giai) {
//        String g = giai.toUpperCase();
//        String m = mien.toUpperCase();
//
//        if (m.contains("BẮC")) {
//            return g.equals("G5") || g.equals("G6") || g.equals("ĐẶC BIỆT");
//        } else if (m.contains("NAM") || m.contains("TRUNG")) {
//            return g.equals("G5") || g.equals("G6") || g.equals("G7") || g.equals("ĐẶC BIỆT");
//        }
//        return false;
//    }
//    private double[] tachTienDanhBaChan(String chuoi) {
//        try {
//            String[] parts = chuoi.split("-");
//            double[] tien = new double[3];
//            for (int i = 0; i < parts.length && i < 3; i++) {
//                tien[i] = Double.parseDouble(parts[i]);
//            }
//            return tien;
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Tiền đánh không hợp lệ cho 3 CHÂN: " + chuoi);
//        }
//    }
////=========TÍNH TIỀN NHỎ===============//
//public double tinhTienNho(Long playerId,String cachDanh, String tienDanh, String mien) {
//    double tienDanhDouble;
//
//
//    try {
//        tienDanhDouble = Double.parseDouble(tienDanh);
//    } catch (NumberFormatException e) {
//        throw new IllegalArgumentException("Giá trị tiền không hợp lệ: " + tienDanh, e);
//    }
//    String loai = chuanHoaCachDanhXuyen(cachDanh);
//    Player player = playerRepository.findById(playerId)
//            .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
//
//    double heSoCachDanh = player.getHeSoCachDanh();
//
//    String m = removeDiacritics(mien).toUpperCase();
//    if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
//        return tienDanhDouble * heSoCachDanh;
//    }
//    return 0;
//}
//
//
//
////================TÍNH TIỀN LỚN =================//
//public double tinhTienLon(Long playerId,String cachDanh, String tienDanh, String mien) {
//    String loai = chuanHoaCachDanhXuyen(cachDanh);
//    double tienDanhDouble;
//
//
//        try {
//            tienDanhDouble = Double.parseDouble(tienDanh);
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Giá trị tiền không hợp lệ: " + tienDanh, e);
//        }
//    Player player = playerRepository.findById(playerId)
//            .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
//
//    double heSoCachDanh = player.getHeSoCachDanh(); // lấy từ DB
//
//    String m = removeDiacritics(mien).toUpperCase();
//    if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
//        return tienDanhDouble * heSoCachDanh;
//    }
//    return 0;
//}
//
//
//    public double getHeSoLonNho(Long playerId, double tien, String mien) {
//        // Lấy player theo id
//        Player player = playerRepository.findById(playerId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
//
//        // Lấy hệ số cách đánh từ DB (ví dụ: 1.95)
//        double heSoCachDanh = player.getHeSoCachDanh();
//
//        // Chuẩn hóa miền
//        String m = removeDiacritics(mien).toUpperCase();
//        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
//            return tien * heSoCachDanh;
//        }
//
//        return 0;
//    }
//
//
//    //============== TÍNH TIỀN XUYÊN ==============//
//
//
//    public double tinhTienXuyen( String cachDanh, String tienDanh, String mien) {
//        // Chuẩn hóa cách đánh
//        String loai = removeDiacritics(cachDanh).toUpperCase().replaceAll("\\s+", "");
//        String m = removeDiacritics(mien).toUpperCase();
//
//        double tienDanhDouble;
//        try {
//            tienDanhDouble = Double.parseDouble(tienDanh);
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Giá trị tiền không hợp lệ: " + tienDanh, e);
//        }
//
//        // Xác định số xuyên (2 → XUYEN2, 3 → XUYEN3 ...)
//        int soXuyen;
//        if (loai.equals("XUYEN2")) soXuyen = 2;
//        else if (loai.equals("XUYEN3")) soXuyen = 3;
//        else if (loai.equals("XUYEN4")) soXuyen = 4;
//        else if (loai.equals("XUYEN5")) soXuyen = 5;
//        else return 0; // Không phải loại xuyên thì thoát luôn
//
//        return tinhTienTheoMienXuyen(soXuyen, tienDanhDouble, m);
//    }
//
//    private double tinhTienTheoMienXuyen(int soXuyen, double tien, String mien) {
//        // Tách riêng từng miền, ghi rõ công thức
//        if (mien.contains("BAC")) {
//            return switch (soXuyen) {
//                case 2 -> tien * 10;   // Miền Bắc Xuyên 2 ăn gấp 10 lần
//                case 3 -> tien * 40;   // Miền Bắc Xuyên 3 ăn gấp 40 lần
//                case 4 -> tien * 100;  // Miền Bắc Xuyên 4 ăn gấp 100 lần
//                case 5 -> tien * 200;  // Miền Bắc Xuyên 5 ăn gấp 200 lần
//                default -> 0;
//            };
//        }
//        else if (mien.contains("TRUNG") || mien.contains("NAM")) {
//            return switch (soXuyen) {
//                case 2 -> tien * 15;   // Miền Trung / Nam Xuyên 2 ăn gấp 15 lần
//                case 3 -> tien * 60;   // Miền Trung / Nam Xuyên 3 ăn gấp 60 lần
//                case 4 -> tien * 120;  // Miền Trung / Nam Xuyên 4 ăn gấp 120 lần
//                case 5 -> tien * 250;  // Miền Trung / Nam Xuyên 5 ăn gấp 250 lần
//                default -> 0;
//            };
//        }
//        return 0;
//    }
//
//
//
//
////=========== HÀM PHỤ =========
//
//
//    private double getHeSoDauDuoiCaCap(double tien, String mien) {
//        String m = removeDiacritics(mien).toUpperCase();
//        if (m.contains("BAC")) return tien * 70 / 5;
//        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70 / 2;
//        return 0;
//    }
//
//
//
//
//    private String removeDiacritics(String input) {
//        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
//        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
//                .matcher(normalized)
//                .replaceAll("");
//    }
//
//
//
//
//    private boolean chuaDu3ChuSo(String soTrung, String soNguoiChoi) {
//        if (soTrung == null || soTrung.length() < 3 || soNguoiChoi.length() != 3) return false;
//        String baSoCuoi = soTrung.substring(soTrung.length() - 3);
//        return baSoCuoi.equals(soNguoiChoi);
//    }
//
//
//
//    public String chuanHoaTenGiai(String giaiRaw) {
//        if (giaiRaw == null) return "";
//        String giai = giaiRaw.trim().toUpperCase()
//                .replace(".", "")
//                .replace("GIẢI", "")
//                .replace("GIAI", "")
//                .replace(" ", "");
//
//        if (giai.contains("DB") || giai.contains("DACBIET") || giai.contains("ĐẶCBIỆT")) {
//            return "ĐẶC BIỆT";
//        }
//
//        if (giai.matches("G[1-8]")) return giai;
//
//        return switch (giai) {
//            case "BAY", "BẢY" -> "G7";
//            case "SAU", "SÁU" -> "G6";
//            case "NAM", "NĂM" -> "G5";
//            case "BON", "BỐN" -> "G4";
//            case "BA" -> "G3";
//            case "HAI" -> "G2";
//            case "NHAT", "NHẤT" -> "G1";
//            default -> giaiRaw.toUpperCase();
//        };
//    }
//    public String chuanHoaCachDanhXuyen(String cachDanh) {
//        if (cachDanh == null) return null;
//
//        // 1. Chuẩn hóa chuỗi gốc
//        String cd = removeDiacritics(cachDanh)      // bỏ dấu tiếng Việt
//                .toUpperCase()                      // viết hoa
//                .replaceAll("[\\s\\.,]+", "");      // bỏ khoảng trắng, dấu chấm, phẩy
//
//        // 2. Thay thế "XIÊN" -> "XUYEN"
//        cd = cd.replace("XIEN", "XUYEN");
//
//        // 3. Trường hợp dạng "2XUYEN" hoặc "3XUYEN" => chuẩn hóa "XUYEN2", "XUYEN3"
//        if (cd.matches("\\d+XUYEN\\d*")) {
//            String so = cd.replaceAll("\\D+", ""); // lấy số
//            return "XUYEN" + so;
//        }
//
//        // 4. Trường hợp dạng "XUYEN2", "XUYEN3" => đã chuẩn
//        if (cd.matches("XUYEN\\d+")) {
//            return cd;
//        }
//
//        // 5. Trường hợp chỉ ghi "XUYEN" hoặc "XIEN" => mặc định là "XUYEN2"
//        if (cd.equals("XUYEN") || cd.equals("XIEN")) {
//            return "XUYEN2";
//        }
//
//        // 6. Fallback: trả về dạng đã chuẩn hóa
//        return cd;
//    }
//
//
//}
package com.example.doxoso.service;

import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.Player;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;
import com.example.doxoso.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TinhTienService {

    @Autowired
    KetQuaMienBacRepository bacRepo;
    @Autowired
    KetQuaMienNamRepository namRepo;
    @Autowired
    KetQuaMienTrungRepository trungRepo;
    @Autowired
    PlayerRepository playerRepository;

    // =========================================================
    // ================ HELPER CHUNG / CHUẨN HÓA ===============
    // =========================================================

    // [NEW] Nhân tiền theo số đài (dùng khi muốn gom 1 lần ở tầng tổng)
    private double scaleByDai(double amount, int soDai) {
        return (soDai <= 0) ? amount : amount * soDai;
    }

    // [NEW] 3 CHÂN: nhân đồng thời 3 ô tiền (bao lô - thường - đặc biệt)
    private double[] scaleTienChuoiBaChan(double[] parts, int soDai) {
        return new double[]{
                scaleByDai(parts[0], soDai),
                scaleByDai(parts[1], soDai),
                scaleByDai(parts[2], soDai)
        };
    }

    // [NEW] parse double an toàn
    private double safeParse(String s, String fieldName) {
        try { return Double.parseDouble(s); }
        catch (Exception e) {
            throw new IllegalArgumentException("Giá trị tiền không hợp lệ cho " + fieldName + ": " + s, e);
        }
    }

    private String removeDiacritics(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("");
    }

    // [UPDATED] chuẩn hóa tên giải → mã ngắn dùng tính tiền
    public String chuanHoaTenGiai(String giaiRaw) {
        if (giaiRaw == null) return "";
        String giai = giaiRaw.trim().toUpperCase()
                .replace(".", "")
                .replace("GIẢI", "")
                .replace("GIAI", "")
                .replace(" ", "");

        if (giai.contains("DB") || giai.contains("DACBIET") || giai.contains("ĐẶCBIỆT")) {
            return "ĐẶC BIỆT";
        }
        if (giai.matches("G[1-8]")) return giai;

        return switch (giai) {
            case "BAY", "BẢY" -> "G7";
            case "SAU", "SÁU" -> "G6";
            case "NAM", "NĂM" -> "G5";
            case "BON", "BỐN" -> "G4";
            case "BA" -> "G3";
            case "HAI" -> "G2";
            case "NHAT", "NHẤT" -> "G1";
            default -> giaiRaw.toUpperCase();
        };
    }

    // [NEW] chuẩn hóa cách đánh XUYÊN về form XUYEN{n}
    public String chuanHoaCachDanhXuyen(String cachDanh) {
        if (cachDanh == null) return null;
        String cd = removeDiacritics(cachDanh)
                .toUpperCase()
                .replaceAll("[\\s\\.,]+", "");
        cd = cd.replace("XIEN", "XUYEN");
        if (cd.matches("\\d+XUYEN\\d*")) {
            String so = cd.replaceAll("\\D+", "");
            return "XUYEN" + so;
        }
        if (cd.matches("XUYEN\\d+")) return cd;
        if (cd.equals("XUYEN") || cd.equals("XIEN")) return "XUYEN2";
        return cd;
    }

    // =========================================================
    // ======================= 2 CHÂN ==========================
    // =========================================================

    // [UNCHANGED] per-đài: truyền đúng soLanTrung của CHÍNH đài đó
    public double tinhTongTien2Chan(String mien, double tienDanh, int soLanTrung) {
        if (soLanTrung <= 0) return 0;
        double tienMotLan = getHeSo2Chan(tienDanh, mien);
        return tienMotLan * soLanTrung;
    }

    // [NEW] overload gom nhiều đài (chỉ dùng nếu muốn nhân tiền theo số đài trước)
    public double tinhTongTien2Chan(String mien, double tienDanh, int soLanTrung, int soDai) {
        if (soLanTrung <= 0) return 0;
        double tienMotLan = getHeSo2Chan(tienDanh, mien);
        return tienMotLan * soLanTrung * Math.max(soDai, 1);
    }

    private double getHeSo2Chan(double tien, String mien) {
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC")) return tien * 70 / 27;
        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70 / 18;
        return 0;
    }

    private String normMien(String mien) {
        if (mien == null) return "";
        String normalized = java.text.Normalizer.normalize(mien, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toUpperCase().trim(); // "MIEN NAM"
    }

// =========================================================
// ===================== ĐẦU / ĐUÔI ========================
// =========================================================

    // per-đài: cho từng đài
    public double tinhTienDauDuoi(boolean trungDau, boolean trungDuoi,
                                  String mienTrungDau, String mienTrungDuoi,
                                  double tienDanhCuaDai) {
        double tien = 0.0;
        if (trungDau && trungDuoi) {
            double tienDau  = tienDanhCuaDai / 2.0;
            double tienDuoi = tienDanhCuaDai / 2.0;
            tien += tinhTienDau(true,  mienTrungDau,  tienDau);
            tien += tinhTienDuoi(true, mienTrungDuoi, tienDuoi);
        } else if (trungDau) {
            tien = tinhTienDau(true,  mienTrungDau,  tienDanhCuaDai);
        } else if (trungDuoi) {
            tien = tinhTienDuoi(true, mienTrungDuoi, tienDanhCuaDai);
        }
        return tien;
    }

    // gom nhiều đài (nếu cần nhân theo số đài trước khi tính)
    public double tinhTienDauDuoi(boolean trungDau, boolean trungDuoi,
                                  String mienTrungDau, String mienTrungDuoi,
                                  double tienDanhCuaDai, int soDai) {
        double tienTheoSoDai = scaleByDai(tienDanhCuaDai, Math.max(soDai, 1));
        return tinhTienDauDuoi(trungDau, trungDuoi, mienTrungDau, mienTrungDuoi, tienTheoSoDai);
    }

    // ====== TÍNH ĐẦU per-đài (robust miền) ======
    double tinhTienDau(boolean trungDau, String mien, double tienDanh) {
        if (!trungDau) return 0.0;
        String m = normMien(mien);
        if (m.contains("BAC")) {                  // MB: G7
            return tienDanh * 70.0 / 4.0;
        } else if (m.contains("TRUNG") || m.contains("NAM")) { // MT/MN: G8
            return tienDanh * 70.0;
        }
        return 0.0;
    }

    // ====== TÍNH ĐUÔI per-đài (robust miền) ======
    double tinhTienDuoi(boolean trungDuoi, String mien, double tienDanh) {
        if (!trungDuoi) return 0.0;
        String m = normMien(mien);
        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
            return tienDanh * 70.0;
        }
        return 0.0;
    }


    // =========================================================
    // ======================= 3 CHÂN ==========================
    // =========================================================

    // [UPDATED] per-đài: tienChuoi = "baoLo-thuong-dacBiet"; giaiTrung = list giải của CHÍNH đài
    public double[] tinhTien3Chan(String tienChuoi, String mien, List<String> giaiTrung) {
        double[] tien = tachTienDanhBaChan(tienChuoi);
        double tienBaoLo = tien[0];
        double tienThuong = tien[1];
        double tienDacBiet = tien[2];

        double tongTien = 0, tongBaoLo = 0, tongThuong = 0, tongDacBiet = 0;

        for (String giai : giaiTrung) {
            String g = chuanHoaTenGiai(giai);

            if (mien.contains("BẮC")) {
                if (g.equals("ĐẶC BIỆT")) {
                    double tienDB = tienDacBiet * 600;
                    double tienBL = tienBaoLo * 600 / 23;
                    double tienT  = tienThuong * 600 / 10;
                    tongDacBiet += tienDB; tongBaoLo += tienBL; tongThuong += tienT;
                    tongTien    += tienDB + tienBL + tienT;
                } else if (isLoThuong(mien, g)) {
                    double tienBL = tienBaoLo * 600 / 23;
                    double tienT  = tienThuong * 600 / 10;
                    tongBaoLo += tienBL; tongThuong += tienT; tongTien += tienBL + tienT;
                } else {
                    double tienBL = tienBaoLo * 600 / 23;
                    tongBaoLo += tienBL; tongTien += tienBL;
                }

            } else if (mien.contains("TRUNG")) {
                if (g.equals("ĐẶC BIỆT")) {
                    double tienDB = tienDacBiet * 600;
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongDacBiet += tienDB; tongBaoLo += tienBL; tongThuong += tienT;
                    tongTien    += tienDB + tienBL + tienT;
                } else if (isLoThuong(mien, g)) {
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongBaoLo += tienBL; tongThuong += tienT; tongTien += tienBL + tienT;
                } else {
                    double tienBL = tienBaoLo * 600 / 17;
                    tongBaoLo += tienBL; tongTien += tienBL;
                }

            } else if (mien.contains("NAM")) {
                if (g.equals("ĐẶC BIỆT")) {
                    double tienDB = tienDacBiet * 600;
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongDacBiet += tienDB; tongBaoLo += tienBL; tongThuong += tienT;
                    tongTien    += tienDB + tienBL + tienT;
                } else if (isLoThuong(mien, g)) {
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongBaoLo += tienBL; tongThuong += tienT; tongTien += tienBL + tienT;
                } else {
                    double tienBL = tienBaoLo * 600 / 17;
                    tongBaoLo += tienBL; tongTien += tienBL;
                }
            }
        }

        return new double[]{ tongTien, tongBaoLo, tongThuong, tongDacBiet };
    }

    // [NEW] overload gom nhiều đài: chỉ dùng nếu bạn gộp list giải của nhiều đài VÀ muốn nhân tiền theo số đài
    public double[] tinhTien3Chan(String tienChuoi, String mien, List<String> giaiTrung, int soDai) {
        double[] base = tachTienDanhBaChan(tienChuoi);
        double[] scaled = scaleTienChuoiBaChan(base, Math.max(soDai, 1));
        double tienBaoLo = scaled[0], tienThuong = scaled[1], tienDacBiet = scaled[2];

        double tongTien = 0, tongBaoLo = 0, tongThuong = 0, tongDacBiet = 0;

        for (String giai : giaiTrung) {
            String g = chuanHoaTenGiai(giai);

            if (mien.contains("BẮC")) {
                if (g.equals("ĐẶC BIỆT")) {
                    double tienDB = tienDacBiet * 600;
                    double tienBL = tienBaoLo * 600 / 23;
                    double tienT  = tienThuong * 600 / 10;
                    tongDacBiet += tienDB; tongBaoLo += tienBL; tongThuong += tienT;
                    tongTien    += tienDB + tienBL + tienT;
                } else if (isLoThuong(mien, g)) {
                    double tienBL = tienBaoLo * 600 / 23;
                    double tienT  = tienThuong * 600 / 10;
                    tongBaoLo += tienBL; tongThuong += tienT; tongTien += tienBL + tienT;
                } else {
                    double tienBL = tienBaoLo * 600 / 23;
                    tongBaoLo += tienBL; tongTien += tienBL;
                }

            } else if (mien.contains("TRUNG")) {
                if (g.equals("ĐẶC BIỆT")) {
                    double tienDB = tienDacBiet * 600;
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongDacBiet += tienDB; tongBaoLo += tienBL; tongThuong += tienT;
                    tongTien    += tienDB + tienBL + tienT;
                } else if (isLoThuong(mien, g)) {
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongBaoLo += tienBL; tongThuong += tienT; tongTien += tienBL + tienT;
                } else {
                    double tienBL = tienBaoLo * 600 / 17;
                    tongBaoLo += tienBL; tongTien += tienBL;
                }

            } else if (mien.contains("NAM")) {
                if (g.equals("ĐẶC BIỆT")) {
                    double tienDB = tienDacBiet * 600;
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongDacBiet += tienDB; tongBaoLo += tienBL; tongThuong += tienT;
                    tongTien    += tienDB + tienBL + tienT;
                } else if (isLoThuong(mien, g)) {
                    double tienBL = tienBaoLo * 600 / 17;
                    double tienT  = tienThuong * 100;
                    tongBaoLo += tienBL; tongThuong += tienT; tongTien += tienBL + tienT;
                } else {
                    double tienBL = tienBaoLo * 600 / 17;
                    tongBaoLo += tienBL; tongTien += tienBL;
                }
            }
        }

        return new double[]{ tongTien, tongBaoLo, tongThuong, tongDacBiet };
    }

    // [UPDATED] chuẩn hóa logic “lô – thường” theo miền
    public boolean isLoThuong(String mien, String giai) {
        String g = giai.toUpperCase();
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC")) {
            return g.equals("G5") || g.equals("G6") || g.equals("ĐẶC BIỆT");
        } else if (m.contains("NAM") || m.contains("TRUNG")) {
            return g.equals("G5") || g.equals("G6") || g.equals("G7") || g.equals("ĐẶC BIỆT");
        }
        return false;
    }

    private double[] tachTienDanhBaChan(String chuoi) {
        try {
            String[] parts = chuoi.split("-");
            double[] tien = new double[3];
            for (int i = 0; i < parts.length && i < 3; i++) {
                tien[i] = Double.parseDouble(parts[i].trim());
            }
            return tien;
        } catch (Exception e) {
            throw new IllegalArgumentException("Tiền đánh không hợp lệ cho 3 CHÂN: " + chuoi);
        }
    }

    // =========================================================
    // ==================== LỚN / NHỎ / XUYÊN =================
    // =========================================================

    // [UNCHANGED] per-đài
    public double tinhTienNho(Long playerId, String cachDanh, String tienDanh, String mien) {
        double tienDanhDouble = safeParse(tienDanh, "tienDanh");
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
        double heSoCachDanh = player.getHeSoCachDanh();
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
            return tienDanhDouble * heSoCachDanh;
        }
        return 0;
    }

    // [NEW] overload gộp nhiều đài (nếu thật sự muốn nhân tiền trước)
    public double tinhTienNho(Long playerId, String cachDanh, String tienDanh, String mien, int soDai) {
        double tienDanhDouble = safeParse(tienDanh, "tienDanh");
        double tienTheoSoDai = scaleByDai(tienDanhDouble, Math.max(soDai, 1));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
        double heSoCachDanh = player.getHeSoCachDanh();
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
            return tienTheoSoDai * heSoCachDanh;
        }
        return 0;
    }

    // [UNCHANGED] per-đài
    public double tinhTienLon(Long playerId, String cachDanh, String tienDanh, String mien) {
        double tienDanhDouble = safeParse(tienDanh, "tienDanh");
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
        double heSoCachDanh = player.getHeSoCachDanh();
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
            return tienDanhDouble * heSoCachDanh;
        }
        return 0;
    }

    // [NEW] overload gộp nhiều đài
    public double tinhTienLon(Long playerId, String cachDanh, String tienDanh, String mien, int soDai) {
        double tienDanhDouble = safeParse(tienDanh, "tienDanh");
        double tienTheoSoDai = scaleByDai(tienDanhDouble, Math.max(soDai, 1));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
        double heSoCachDanh = player.getHeSoCachDanh();
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
            return tienTheoSoDai * heSoCachDanh;
        }
        return 0;
    }

    // [UPDATED] per-đài
    public double tinhTienXuyen(String cachDanh, String tienDanh, String mien) {
        String loai = removeDiacritics(cachDanh).toUpperCase().replaceAll("\\s+", "");
        String m = removeDiacritics(mien).toUpperCase();
        double tienDanhDouble = safeParse(tienDanh, "tienDanh");

        int soXuyen = switch (loai) {
            case "XUYEN2" -> 2;
            case "XUYEN3" -> 3;
            case "XUYEN4" -> 4;
            case "XUYEN5" -> 5;
            default -> 0;
        };
        if (soXuyen == 0) return 0;
        return tinhTienTheoMienXuyen(soXuyen, tienDanhDouble, m);
    }

    // [NEW] overload gộp nhiều đài
    public double tinhTienXuyen(String cachDanh, String tienDanh, String mien, int soDai) {
        String loai = removeDiacritics(cachDanh).toUpperCase().replaceAll("\\s+", "");
        String m = removeDiacritics(mien).toUpperCase();
        double tienDanhDouble = safeParse(tienDanh, "tienDanh");
        double tienTheoSoDai = scaleByDai(tienDanhDouble, Math.max(soDai, 1));

        int soXuyen = switch (loai) {
            case "XUYEN2" -> 2;
            case "XUYEN3" -> 3;
            case "XUYEN4" -> 4;
            case "XUYEN5" -> 5;
            default -> 0;
        };
        if (soXuyen == 0) return 0;
        return tinhTienTheoMienXuyen(soXuyen, tienTheoSoDai, m);
    }

    private double tinhTienTheoMienXuyen(int soXuyen, double tien, String mien) {
        if (mien.contains("BAC")) {
            return switch (soXuyen) {
                case 2 -> tien * 10;
                case 3 -> tien * 40;
                case 4 -> tien * 100;
                case 5 -> tien * 200;
                default -> 0;
            };
        } else if (mien.contains("TRUNG") || mien.contains("NAM")) {
            return switch (soXuyen) {
                case 2 -> tien * 15;
                case 3 -> tien * 60;
                case 4 -> tien * 120;
                case 5 -> tien * 250;
                default -> 0;
            };
        }
        return 0;
    }

    // =========================================================
    // =================== HÀM PHỤ CHO 3 CHÂN ==================
    // =========================================================

    // [NEW] Khớp 3 số cuối của số trúng với số người chơi (3 chữ số)
    public boolean khopBaSoCuoi(String soTrung, String soNguoiChoi) {
        if (soTrung == null || soNguoiChoi == null) return false;
        if (soNguoiChoi.length() != 3 || soTrung.length() < 3) return false;
        String baSoCuoi = soTrung.substring(soTrung.length() - 3);
        return baSoCuoi.equals(soNguoiChoi);
    }

    // [NEW] Hệ số tổng hợp cho ĐẦU-ĐUÔI cả cặp (giữ để tái sử dụng nếu cần)
    private double getHeSoDauDuoiCaCap(double tien, String mienGoc) {
        String m = removeDiacritics(mienGoc).toUpperCase();
        if (m.contains("BAC")) return tien * 70 / 5;
        if (m.contains("TRUNG") || m.contains("NAM")) return tien * 70 / 2;
        return 0;
    }

    // [NEW] LỚN/ NHỎ tổng quát theo hệ số người chơi (nếu muốn dùng trực tiếp)
    public double getHeSoLonNho(Long playerId, double tien, String mien) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy player với id: " + playerId));
        double heSoCachDanh = player.getHeSoCachDanh();
        String m = removeDiacritics(mien).toUpperCase();
        if (m.contains("BAC") || m.contains("TRUNG") || m.contains("NAM")) {
            return tien * heSoCachDanh;
        }
        return 0;
    }
}
