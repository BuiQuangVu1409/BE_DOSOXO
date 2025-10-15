// com.example.doxoso.controller.TongTienAnThuaController.java
package com.example.doxoso.controller;

import com.example.doxoso.model.TongTienAnThuaMien;
import com.example.doxoso.service.TongTienAnThuaMienService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/an-thua")
public class TongTienAnThuaMienController {

    private final TongTienAnThuaMienService service;

    /**
     * GET /tong-tien-an-thua/{playerId}/{ngay}?playerName=...
     * Trả về danh sách ăn/thua theo từng miền cho 1 player trong 1 ngày.
     */
    @GetMapping("/{playerId}/{ngay}")
    public List<TongTienAnThuaMien> getByPlayerAndNgay(
            @PathVariable Long playerId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) String playerName
    ) {
        return service.tinh(playerId, playerName, ngay);
    }
}
