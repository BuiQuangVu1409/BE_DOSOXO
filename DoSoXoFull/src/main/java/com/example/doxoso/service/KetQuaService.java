package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;

import com.example.doxoso.model.Bet;
import com.example.doxoso.repository.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

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
        String cachDanhChuanHoa = chuanHoa(cachDanh);

        return doiChieuTatCaSo().stream()
                .filter(dto -> {
                    boolean trung = dto.isTrung();
                    boolean hopLeKetQua = trungOrTrat.equalsIgnoreCase("trung") ? trung : !trung;
                    String cd = chuanHoa(dto.getCachDanh());
                    return hopLeKetQua && cd.contains(cachDanhChuanHoa);
                })
                .collect(Collectors.toList());
    }

    private String chuanHoa(String text) {
        return text == null ? "" :
                java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // bỏ dấu tiếng Việt
                        .replaceAll("\\s+", "") // xoá khoảng trắng
                        .toLowerCase(); // viết thường
    }
    /**
     * Dò kết quả và lưu DB cho danh sách số của 1 player.
     */
    public List<DoiChieuKetQuaDto> doKetQua(List<Bet> soNguoiChoiList) {
        List<DoiChieuKetQuaDto> ketQuaList = new ArrayList<>();

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