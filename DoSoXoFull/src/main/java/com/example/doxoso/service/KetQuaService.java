//package com.example.doxoso.service;
//
//import com.example.doxoso.model.Bet;
//import com.example.doxoso.model.DoiChieuKetQuaDto;
//
//
//import com.example.doxoso.repository.BetRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//
//import java.util.ArrayList;
//import java.util.List;
//
//import java.util.stream.Collectors;
//
//@Service
//public class KetQuaService implements IKetQuaService {
//
//    @Autowired
//    private BetRepository soNguoiChoiRepo;
//    @Autowired
//    private KiemTraKetQuaService kiemTraKetQuaService;
//@Autowired
//private KetQuaNguoiChoiService ketQuaNguoiChoiService;
//
//
//    @Override
//    public List<DoiChieuKetQuaDto> doiChieuTatCaSo() {
//        return soNguoiChoiRepo.findAll()
//                .stream()
//                .map(kiemTraKetQuaService::kiemTraSo)
//                .collect(Collectors.toList());
//    }
//    @Override
//    public List<DoiChieuKetQuaDto> layDanhSachSoTrung() {
//        return doiChieuTatCaSo().stream()
//                .filter(DoiChieuKetQuaDto::isTrung)
//                .collect(Collectors.toList());
//    }
//    @Override
//    public List<DoiChieuKetQuaDto> layDanhSachSoTrat() {
//        return doiChieuTatCaSo().stream()
//                .filter(dto -> !dto.isTrung())
//                .collect(Collectors.toList());
//    }
//
//
//    @Override
//    public List<DoiChieuKetQuaDto> locTheoKetQuaVaCachDanh(String trungOrTrat, String cachDanh) {
//        String cachDanhChuanHoa = chuanHoa(cachDanh);
//
//        return doiChieuTatCaSo().stream()
//                .filter(dto -> {
//                    boolean trung = dto.isTrung();
//                    boolean hopLeKetQua = trungOrTrat.equalsIgnoreCase("trung") ? trung : !trung;
//                    String cd = chuanHoa(dto.getCachDanh());
//                    return hopLeKetQua && cd.contains(cachDanhChuanHoa);
//                })
//                .collect(Collectors.toList());
//    }
//
//    private String chuanHoa(String text) {
//        return text == null ? "" :
//                java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
//                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // bỏ dấu tiếng Việt
//                        .replaceAll("\\s+", "") // xoá khoảng trắng
//                        .toLowerCase(); // viết thường
//    }
//    /**
//     * Dò kết quả và lưu DB cho danh sách số của 1 player.
//     */
//    public List<DoiChieuKetQuaDto> doKetQua(List<Bet> soNguoiChoiList) {
//        List<DoiChieuKetQuaDto> ketQuaList = new ArrayList<>();
//
//        for (Bet bet : soNguoiChoiList) {
//            // B1: Dò số
//            DoiChieuKetQuaDto dto = kiemTraKetQuaService.kiemTraSo(bet);
//
//            // B2: Lưu kết quả
//            ketQuaNguoiChoiService.luuKetQua(bet, dto);
//
//            ketQuaList.add(dto);
//        }
//
//        return ketQuaList;
//    }
//
//}
package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.repository.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class KetQuaService implements IKetQuaService {

    @Autowired
    private BetRepository soNguoiChoiRepo;

    @Autowired
    private KiemTraKetQuaService kiemTraKetQuaService;

    @Autowired
    private KetQuaNguoiChoiService ketQuaNguoiChoiService;

    @Override
    public List<DoiChieuKetQuaDto> doiChieuTatCaSo() {
        return soNguoiChoiRepo.findAll()
                .stream()
                .map(kiemTraKetQuaService::kiemTraSo)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoiChieuKetQuaDto> layDanhSachSoTrung() {
        return doiChieuTatCaSo().stream()
                .filter(DoiChieuKetQuaDto::isTrung)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoiChieuKetQuaDto> layDanhSachSoTrat() {
        return doiChieuTatCaSo().stream()
                .filter(dto -> !dto.isTrung())
                .collect(Collectors.toList());
    }

    @Override
    public List<DoiChieuKetQuaDto> locTheoKetQuaVaCachDanh(String trungOrTrat, String cachDanh) {
        final String ketQuaKey = (trungOrTrat == null) ? "" : trungOrTrat.trim().toLowerCase(Locale.ROOT);
        if (!ketQuaKey.equals("trung") && !ketQuaKey.equals("trat")) {
            // nếu controller truyền sai, trả rỗng cho an toàn
            return List.of();
        }

        final String cachDanhKey = chuanHoaCachDanh(cachDanh);

        return doiChieuTatCaSo().stream()
                .filter(dto -> {
                    boolean hopLeKetQua = ketQuaKey.equals("trung") ? dto.isTrung() : !dto.isTrung();

                    // nếu không truyền cachDanh thì chỉ lọc theo trúng/trật
                    if (cachDanhKey.isEmpty()) return hopLeKetQua;

                    String cdDto = chuanHoaCachDanh(dto.getCachDanh());

                    // ✅ so sánh CHÍNH XÁC, tránh match bậy
                    return hopLeKetQua && cdDto.equals(cachDanhKey);
                })
                .collect(Collectors.toList());
    }

    /**
     * Chuẩn hoá mạnh:
     * - bỏ dấu tiếng Việt
     * - chuyển đ/Đ -> d/D
     * - lowercase
     * - xoá toàn bộ ký tự không phải chữ/số (space, _, -, ...)
     *
     * Ví dụ:
     *  "CHẴN ĐẦU"  -> "chandau"
     *  "CHAN_DAU"  -> "chandau"
     *  "le-duoi"   -> "leduoi"
     */
    private String chuanHoaCachDanh(String text) {
        if (text == null) return "";
        String s = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();

        // ✅ xoá hết ký tự không phải chữ/số
        s = s.replaceAll("[^a-z0-9]+", "");
        return s;
    }

    /**
     * Dò kết quả và lưu DB cho danh sách số của 1 player.
     */
    public List<DoiChieuKetQuaDto> doKetQua(List<Bet> soNguoiChoiList) {
        List<DoiChieuKetQuaDto> ketQuaList = new ArrayList<>();
        if (soNguoiChoiList == null || soNguoiChoiList.isEmpty()) return ketQuaList;

        for (Bet bet : soNguoiChoiList) {
            // B1: Dò số
            DoiChieuKetQuaDto dto = kiemTraKetQuaService.kiemTraSo(bet);

            // B2: Lưu kết quả
            ketQuaNguoiChoiService.luuKetQua(bet, dto);

            ketQuaList.add(dto);
        }

        return ketQuaList;
    }
}
