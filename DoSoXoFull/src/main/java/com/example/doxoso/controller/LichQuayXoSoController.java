package com.example.doxoso.controller;

import com.example.doxoso.model.LichQuayXoSo;
import com.example.doxoso.model.LichDoMienDto;
import com.example.doxoso.service.LichQuayXoSoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping("/lich")
public class LichQuayXoSoController {

    @Autowired
    private LichQuayXoSoService lichQuayXoSoService;

    /**
     * Lịch quay "chuẩn" theo thứ (static) – dùng cho hiển thị nhanh:
     *  - Nếu không truyền ngày -> mặc định hôm nay
     *
     *  Ví dụ:
     *      GET http://localhost:8080/lich
     *      GET http://localhost:8080/lich?ngay=2025-06-22
     */
    @GetMapping
    public LichQuayXoSo traCuuLichTheoNgay(
            @RequestParam(value = "ngay", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {

        if (ngay == null) {
            ngay = LocalDate.now(); // tự động dùng ngày hôm nay
        }
        return lichQuayXoSoService.traCuuTheoNgay(ngay);
    }

    /**
     * Lịch quay THỰC TẾ dựa trên dữ liệu 3 bảng KetQuaMienBac/Trung/Nam
     * trong khoảng ngày [from, to].
     *
     *  Ví dụ:
     *      GET http://localhost:8080/lich/db?from=2025-09-01&to=2025-09-30
     */
    @GetMapping("/db")
    public List<LichQuayXoSo> traCuuLichTheoKhoangNgay(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return lichQuayXoSoService.traCuuLichQuayTheoKetQua(from, to);
    }

    /**
     * Lịch quay THỰC TẾ cho MỘT NGÀY (dùng DB)
     *
     *  Ví dụ:
     *      GET http://localhost:8080/lich/db/ngay?ngay=2025-09-15
     */
    @GetMapping("/db/ngay")
    public List<LichQuayXoSo> traCuuLichMotNgay(
            @RequestParam("ngay")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {

        // from = to = cùng 1 ngày -> list chỉ có tối đa 1 phần tử
        return lichQuayXoSoService.traCuuLichQuayTheoKetQua(ngay, ngay);
    }

    /**
     * BẢNG KẾT QUẢ CHI TIẾT (ĐẶC BIỆT, G1..G7) cho 1 ngày + 1 miền.
     *  - mien: MB | MT | MN
     *
     *  Ví dụ:
     *      GET http://localhost:8080/lich/bang-ket-qua?ngay=2025-09-15&mien=MN
     */
    @GetMapping("/bang-ket-qua")
    public LichDoMienDto getBangKetQua(
            @RequestParam("ngay")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam("mien") String mien) {

        return lichQuayXoSoService.getBangKetQua(ngay, mien);
    }
}
