package com.example.doxoso.controller;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.KetQuaMienBacRepository;
import com.example.doxoso.repository.KetQuaMienNamRepository;
import com.example.doxoso.repository.KetQuaMienTrungRepository;


import com.example.doxoso.service.*;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/xoso")
public class KetQuaController {

    @Autowired
    private KetQuaService ketQuaService;

    @Autowired
    private KiemTraKetQuaService kiemTraKetQuaService;


    @Autowired
    private KetQuaMienBacRepository bacRepo;

    @Autowired
    private KetQuaMienTrungRepository trungRepo;

    @Autowired
    private KetQuaMienNamRepository namRepo;

    @Autowired
    BetService betService;


//GIAO DIỆN


    @PostMapping("/doiso")
    public Object doSo(@RequestBody Bet bet) {
        return kiemTraKetQuaService.kiemTraSo(bet);
    }


    @GetMapping("/ketqua")
    public List<KetQuaTheoDaiDto> layKetQuaTheoNgayVaLoc(
            @RequestParam("ngay") String ngayStr,
            @RequestParam(name = "mien", required = false) String mien,
            @RequestParam(name = "dai", required = false) String tenDai) {

        LocalDate ngay;
        try {
            ngay = LocalDate.parse(ngayStr);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày không hợp lệ: " + ngayStr);
        }

        List<KetQuaTheoDaiDto> ketQua = new ArrayList<>();

        if (mien == null || mien.equalsIgnoreCase("MIỀN BẮC")) {
            List<KetQuaMienBac> mb = bacRepo.findAllByNgay(ngay);
            if (!mb.isEmpty()) {
                if (tenDai == null || "HÀ NỘI".equalsIgnoreCase(tenDai)) {
                    ketQua.add(new KetQuaTheoDaiDto("MIỀN BẮC", "HÀ NỘI", mb));
                }
            }
        }

        if (mien == null || mien.equalsIgnoreCase("MIỀN TRUNG")) {
            trungRepo.findAllByNgay(ngay).stream()
                    .filter(kq -> tenDai == null || kq.getTenDai().equalsIgnoreCase(tenDai))
                    .collect(Collectors.groupingBy(KetQuaMienTrung::getTenDai))
                    .forEach((dai, ds) -> ketQua.add(new KetQuaTheoDaiDto("MIỀN TRUNG", dai, ds)));
        }

        if (mien == null || mien.equalsIgnoreCase("MIỀN NAM")) {
            namRepo.findAllByNgay(ngay).stream()
                    .filter(kq -> tenDai == null || kq.getTenDai().equalsIgnoreCase(tenDai))
                    .collect(Collectors.groupingBy(KetQuaMienNam::getTenDai))
                    .forEach((dai, ds) -> ketQua.add(new KetQuaTheoDaiDto("MIỀN NAM", dai, ds)));
        }

        return ketQua;
    }

///ketqua?ngay=2025-08-11&mien=MIỀN NAM


    /**
     * Trả về danh sách đã đối chiếu tất cả số người chơi
     */
    @GetMapping("/doi-chieu")
    public List<DoiChieuKetQuaDto> doiChieuTatCa() {
        return ketQuaService.doiChieuTatCaSo();
    }

    /**
     * Trả về danh sách số trúng
     */
    @GetMapping("/trung")
    public List<DoiChieuKetQuaDto> danhSachTrung() {
        return ketQuaService.layDanhSachSoTrung();
    }

    /**
     * Trả về danh sách số trật
     */
    @GetMapping("/trat")
    public List<DoiChieuKetQuaDto> danhSachTrat() {
        return ketQuaService.layDanhSachSoTrat();
    }


    /**
     * Đối chiếu 1 số người chơi cụ thể (POST JSON)
     */
    @PostMapping("/doi-chieu")
    public DoiChieuKetQuaDto doiChieuMotSo(@RequestBody Bet bet) {
        return kiemTraKetQuaService.kiemTraSo(bet);
    }


    @GetMapping("/{ketqua}/{cachdanh}")
    public List<DoiChieuKetQuaDto> locTheoKetQuaVaCachDanh(
            @PathVariable("ketqua") String ketqua,
            @PathVariable("cachdanh") String cachDanh
    ) {

        String k = Normalizer.normalize(cachDanh, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "");

        switch (k) {
            case "dau"      -> cachDanh = "đầu";
            case "duoi"     -> cachDanh = "đuôi";
            case "dauduoi"  -> cachDanh = "đầu đuôi";
            case "lon"      -> cachDanh = "lớn";
            case "nho"      -> cachDanh = "nhỏ";
            case "2chan"    -> cachDanh = "2 chân";
            case "3chan"    -> cachDanh = "3 chân";
            case "xuyen2"    -> cachDanh = "xuyên 2";
            case "xuyen3"    -> cachDanh = "xuyên 3";
            case "xuyen4"    -> cachDanh = "xuyên 4";
            case "xuyen5"    -> cachDanh = "xuyên 5";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "cachdanh không hợp lệ: " + cachDanh
            );
        }

        return ketQuaService.locTheoKetQuaVaCachDanh(ketqua, cachDanh);
    }



}
//
//    @GetMapping("/doketqua/{playerId}")
//    public ResponseEntity<List<DoiChieuKetQuaDto>> doKetQua(@PathVariable Long playerId) {
//        // Lấy danh sách số đã đánh của player
//        List<SoNguoiChoi> soNguoiChoiList = soNguoiChoiService.getSoNguoiChoiByPlayerId(playerId);
//
//        if (soNguoiChoiList.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Collections.emptyList());
//        }
//
//        // Dò kết quả
//        List<DoiChieuKetQuaDto> ketQua = kiemTraKetQuaService.doKetQua(soNguoiChoiList);
//
//        return ResponseEntity.ok(ketQua);
//
//    }


//    @GetMapping("/{id}")
//    public ResponseEntity<?> getPlayerWithSoNguoiChoi(@PathVariable Long id) {
//        return playerRepository.findWithSoNguoiChoiById(id)
//                .<ResponseEntity<?>>map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of("message", "Không tìm thấy Player với id = " + id)));
//    }