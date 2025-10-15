package com.example.doxoso.controller;

import com.example.doxoso.model.LichQuayXoSo;
import com.example.doxoso.service.LichQuayXoSoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})

@RestController
@RequestMapping("/lich")
public class LichQuayXoSoController {

    @Autowired
    private LichQuayXoSoService lichQuayXoSoService;
@GetMapping
public LichQuayXoSo traCuuLichTheoNgay(
        @RequestParam(value = "ngay", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
    if (ngay == null) {
        ngay = LocalDate.now(); // tự động dùng ngày hôm nay
    }
    return lichQuayXoSoService.traCuuTheoNgay(ngay);
}

}



//http://localhost:8080/lich?ngay=2025-06-22



