


package com.example.doxoso.controller;

import com.example.doxoso.model.TongHopHoaHongLonNhoDto;
import com.example.doxoso.service.TongHopHoaHongLonNhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
@RestController
@RequestMapping({"/api/hh-lonnho", "/api/hh-lon-nho"}) // hỗ trợ cả 2 base path
@RequiredArgsConstructor
public class TongTienHHLonNhoController {

    private final TongHopHoaHongLonNhoService service;

    // nhận cả hai path: /player/{id} và /lon-nho/player/{id}
    @GetMapping({"/player/{playerId}", "/lon-nho/player/{playerId}"})
    public TongHopHoaHongLonNhoDto tongHopLonNhoTrongNgay(
            @PathVariable Long playerId,
            @RequestParam("ngay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(value = "playerName", required = false) String playerName
    ) {
        return service.tongHopMotNgay(playerId, playerName, ngay);
    }
}




//http://localhost:8080/api/hh-lonnho/lon-nho/player/8?ngay=2025-06-25