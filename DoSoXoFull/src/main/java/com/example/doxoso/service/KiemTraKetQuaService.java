package com.example.doxoso.service;

import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.Bet;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KiemTraKetQuaService {

    @Autowired
    private TinhTienService tinhTienService;
    @Autowired
    private ChuyenDoiNgayService chuyenDoiNgayService;
    @Autowired
    private DanhSachDaiTheoMienService danhSachDaiTheoMienService;
    @Autowired
    private XuyenService xuyenService;
    @Autowired
    private HaiChanService haiChanService;
    @Autowired
    private BaChanService baChanService;
    @Autowired
    private DauService dauService;
    @Autowired
    private DuoiService duoiService;
    @Autowired
    private DauDuoiService dauDuoiService;
    @Autowired
    private LonService lonService;
    @Autowired
    private NhoService nhoService;

    // ❌ BỎ KetQuaNguoiChoiService để tránh vòng lặp
    // @Autowired private KetQuaNguoiChoiService ketQuaNguoiChoiService;

    /**
     * Dò số cho 1 người chơi, trả về DTO kết quả.
     */
    public DoiChieuKetQuaDto kiemTraSo(Bet bet) {
        if (bet == null || bet.getNgay() == null) {
            throw new IllegalArgumentException("Thông tin người chơi không hợp lệ");
        }

        DoiChieuKetQuaDto dto = new DoiChieuKetQuaDto();
        dto.setThu(chuyenDoiNgayService.chuyenDoiThu(bet.getNgay()));
        dto.setSoDanh(bet.getSoDanh());
        dto.setTenDai(bet.getDai());
        dto.setMien(bet.getMien());
        dto.setNgay(bet.getNgay());
        dto.setCachDanh(bet.getCachDanh());
        dto.setTienDanh(bet.getSoTien());
        dto.setDanhSachDai(layDanhSachDaiTuCachDanh(bet));

        String cachDanhChuanHoa = chuanHoaCachDanhTheoMien(bet.getCachDanh());

        // ⚡ Logic xử lý các cách đánh (3CHAN, 2CHAN, XUYEN, DAU, DUOI, DAUDUOI, LON, NHO)
        // Giữ nguyên như bạn viết, mình chỉ bỏ phần lưu DB ra ngoài.
        if (cachDanhChuanHoa.equals("3CHAN")) {
            return xuLy3Chan(bet, dto);
        }
        if (cachDanhChuanHoa.equals("2CHAN")) {
            return xuLy2Chan(bet, dto);
        }
        if (xuyenService.laCachDanhXuyen(cachDanhChuanHoa)) {
            return xuLyXuyen(bet, dto);
        }
        if (cachDanhChuanHoa.equals("DAU")) {
            return dauService.xuLyDau(bet.getSoDanh(), bet.getMien(), bet.getNgay(), bet.getSoTien(), bet.getDai());
        }
        if (cachDanhChuanHoa.equals("DUOI")) {
            return duoiService.xuLyDuoi(bet.getSoDanh(), bet.getMien(), bet.getNgay(), bet.getSoTien(), bet.getDai());
        }
        if (cachDanhChuanHoa.equals("DAUDUOI")) {
            return dauDuoiService.xuLyDauDuoi(bet.getSoDanh(), bet.getMien(), bet.getNgay(), bet.getSoTien(), bet.getDai());
        }
        if (cachDanhChuanHoa.equals("LON")) {
            return lonService.xuLyLon(bet.getPlayer().getId(), bet.getSoDanh(), bet.getMien(), bet.getDai(), bet.getNgay(), bet.getSoTien());
        }
        if (cachDanhChuanHoa.equals("NHO")) {
            return nhoService.xuLyNho(bet.getPlayer().getId(), bet.getSoDanh(), bet.getMien(), bet.getDai(), bet.getNgay(), bet.getSoTien());
        }

        return dto;
    }

    // ===================== CÁC HÀM XỬ LÝ RIÊNG =====================

    private DoiChieuKetQuaDto xuLy3Chan(Bet bet, DoiChieuKetQuaDto dto) {
        dto.setCachTrung("3 chân");
        DoiChieuKetQuaDto ketQuaChiTiet = baChanService.xuLyBaChan(bet);
        dto.setKetQuaTungDai(ketQuaChiTiet.getKetQuaTungDai());
        dto.setDanhSachDai(
                ketQuaChiTiet.getKetQuaTungDai().stream().map(DoiChieuKetQuaDto.KetQuaTheoDai::getTenDai).collect(Collectors.toList())
        );

        if (ketQuaChiTiet.isTrung()) {
            dto.setTrung(true);
            double tongTien = 0, tongBaoLo = 0, tongThuong = 0, tongDacBiet = 0;

            for (DoiChieuKetQuaDto.KetQuaTheoDai dai : ketQuaChiTiet.getKetQuaTungDai()) {
                if (dai.isTrung()) {
                    double[] tienTrung = tinhTienService.tinhTien3Chan(bet.getSoTien(), dai.getMien(), dai.getGiaiTrung());
                    dai.setTienTrung(tienTrung[0]);
                    tongTien += tienTrung[0];
                    tongBaoLo += tienTrung[1];
                    tongThuong += tienTrung[2];
                    tongDacBiet += tienTrung[3];
                } else {
                    dai.setTienTrung(0.0);
                }
            }
            dto.setTienTrung(tongTien);
            dto.setTienTrungBaoLo(tongBaoLo);
            dto.setTienTrungThuong(tongThuong);
            dto.setTienTrungDacBiet(tongDacBiet);
            dto.setGiaiTrung(
                    ketQuaChiTiet.getKetQuaTungDai().stream().filter(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung)
                            .map(dai -> dai.getTenDai() + " (" + dai.getSoLanTrung() + " lần)")
                            .collect(Collectors.joining(", "))
            );
        } else {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setTienTrungBaoLo(0.0);
            dto.setTienTrungThuong(0.0);
            dto.setTienTrungDacBiet(0.0);
            dto.setSaiLyDo(List.of("Không trúng 3 chân"));
        }
        return dto;
    }

    private DoiChieuKetQuaDto xuLy2Chan(Bet bet, DoiChieuKetQuaDto dto) {
        dto.setCachTrung("2 chân");
        DoiChieuKetQuaDto ketQuaChiTiet = haiChanService.traVeKetQuaChiTiet2Chan(bet);
        dto.setKetQuaTungDai(ketQuaChiTiet.getKetQuaTungDai());
        dto.setDanhSachDai(ketQuaChiTiet.getKetQuaTungDai().stream().map(DoiChieuKetQuaDto.KetQuaTheoDai::getTenDai).toList());

        if (ketQuaChiTiet.isTrung()) {
            dto.setTrung(true);
            double tongTien = 0;
            for (DoiChieuKetQuaDto.KetQuaTheoDai dai : ketQuaChiTiet.getKetQuaTungDai()) {
                if (dai.isTrung()) {
                    double tienTrung = tinhTienService.tinhTongTien2Chan(dai.getMien(), Double.parseDouble(bet.getSoTien()), dai.getSoLanTrung());
                    dai.setTienTrung(tienTrung);
                    tongTien += tienTrung;
                } else {
                    dai.setTienTrung(0.0);
                }
            }
            dto.setTienTrung(tongTien);
            dto.setGiaiTrung(
                    ketQuaChiTiet.getKetQuaTungDai().stream().filter(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung)
                            .map(dai -> dai.getTenDai() + " (" + dai.getSoLanTrung() + " lần)")
                            .collect(Collectors.joining(", "))
            );
        } else {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không trúng 2 chân"));
        }
        return dto;
    }

    private DoiChieuKetQuaDto xuLyXuyen(Bet bet, DoiChieuKetQuaDto dto) {
        DoiChieuKetQuaDto xuyenDto = xuyenService.xuLyXuyen(bet);
        dto.setKetQuaTungDai(xuyenDto.getKetQuaTungDai());
        List<DoiChieuKetQuaDto.KetQuaTheoDai> daiTrung = xuyenDto.getKetQuaTungDai().stream().filter(DoiChieuKetQuaDto.KetQuaTheoDai::isTrung).toList();

        if (!daiTrung.isEmpty()) {
            dto.setTrung(true);
            dto.setGiaiTrung("Trúng " + bet.getCachDanh() + " tại " +
                    daiTrung.stream().map(DoiChieuKetQuaDto.KetQuaTheoDai::getTenDai).collect(Collectors.joining(", ")));
            double tongTien = daiTrung.stream().mapToDouble(d ->
                    tinhTienService.tinhTienXuyen(bet.getCachDanh(), bet.getSoTien(), bet.getMien())).sum();
            dto.setTienTrung(tongTien);
        } else {
            dto.setTrung(false);
            dto.setTienTrung(0.0);
            dto.setSaiLyDo(List.of("Không trúng " + bet.getCachDanh()));
        }
        return dto;
    }

    // ===================== UTIL =====================

    private List<String> layDanhSachDaiTuCachDanh(Bet bet) {
        int soLuongDai = tachSoLuongDai(bet.getCachDanh());
        return danhSachDaiTheoMienService.layDanhSachDaiTheoSoLuong(bet.getMien(), soLuongDai, bet.getNgay(), chuyenDoiNgayService);
    }

    private int tachSoLuongDai(String cachDanh) {
        if (cachDanh == null) return 0;
        Matcher matcher = Pattern.compile("(\\d+)\\s*đài", Pattern.CASE_INSENSITIVE).matcher(cachDanh);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String chuanHoaCachDanhTheoMien(String cachDanh) {
        if (cachDanh == null) return "";
        return removeDiacritics(cachDanh).toUpperCase().trim().replaceAll("[\\s\\.,;:]+", "");
    }

    private String removeDiacritics(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
    }
}
