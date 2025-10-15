package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
import com.example.doxoso.model.Bet;
import com.example.doxoso.model.KetQuaNguoiChoi;
import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KetQuaNguoiChoiService {

    private final KetQuaNguoiChoiRepository repository;
    private final ObjectMapper objectMapper;

    // ===================== CRUD QUERY =====================

    public List<KetQuaNguoiChoi> getByPlayerId(Long playerId) {
        return repository.findByPlayerId(playerId);
    }

    public List<KetQuaNguoiChoi> getByPlayerName(String playerName) {
        return repository.findByPlayerName(playerName);
    }

    public List<KetQuaNguoiChoi> getByNgayChoi(LocalDate ngayChoi) {
        return repository.findByNgayChoi(ngayChoi);
    }

    public List<KetQuaNguoiChoi> getByPlayerIdAndNgay(Long playerId, LocalDate ngayChoi) {
        return repository.findByPlayerIdAndNgayChoi(playerId, ngayChoi);
    }

    public List<KetQuaNguoiChoi> getByPlayerNameAndNgay(String playerName, LocalDate ngayChoi) {
        return repository.findByPlayerNameAndNgayChoi(playerName, ngayChoi);
    }

    public List<KetQuaNguoiChoi> getKetQuaTrongKhoang(LocalDate start, LocalDate end) {
        return repository.findByNgayChoiTuNgay(start, end);
    }

    // ===================== SAVE LOGIC =====================

    /**
     * Lưu kết quả dò của 1 người chơi (theo từng số & đài).
     * YÊU CẦU: Mỗi "tin" (bản ghi SoNguoiChoi) chỉ được lưu đúng 1 lần.
     */
    public void luuKetQua(Bet bet, DoiChieuKetQuaDto dto) {
        // NEW: Nếu tin này đã từng lưu kết quả → bỏ qua (đúng yêu cầu 5 → +3 = 8, không quay lại 5 cũ)
        if (repository.existsBySourceSoId(bet.getId())) { // NEW
            return;                                       // NEW
        }                                                // NEW

        List<KetQuaNguoiChoi> toSave = new ArrayList<>();

        if (dto.getKetQuaTungDai() != null && !dto.getKetQuaTungDai().isEmpty()) {
            // có chi tiết từng đài
            for (DoiChieuKetQuaDto.KetQuaTheoDai kq : dto.getKetQuaTungDai()) {
                KetQuaNguoiChoi e = buildEntityFromKetQuaTheoDai(bet, dto, kq); // NEW (sẽ set sourceSoId)
                toSave.add(e);
            }
        } else {
            // không có chi tiết đài → lưu bản tổng quát (đánh dấu summary=true để phân biệt)
            KetQuaNguoiChoi e = buildEntityFromTongKet(bet, dto); // NEW (sẽ set sourceSoId + summary)
            toSave.add(e);
        }

        repository.saveAll(toSave);
    }

    /**
     * Lưu kết quả cho danh sách người chơi và dto đã dò xong.
     */
    public void saveAllKetQua(List<Bet> soNguoiChoiList, List<DoiChieuKetQuaDto> ketQuaDtos) {
        for (int i = 0; i < soNguoiChoiList.size(); i++) {
            luuKetQua(soNguoiChoiList.get(i), ketQuaDtos.get(i));
        }
    }

    // ===================== HELPER BUILD ENTITY =====================

    private KetQuaNguoiChoi buildEntityFromKetQuaTheoDai(
            Bet bet,
            DoiChieuKetQuaDto dto,
            DoiChieuKetQuaDto.KetQuaTheoDai kq
    ) {
        KetQuaNguoiChoi entity = new KetQuaNguoiChoi();

        // Player info
        entity.setPlayerId(bet.getPlayer().getId());
        entity.setPlayerName(bet.getPlayer().getName());
        entity.setHoaHong(bet.getPlayer().getHoaHong());
        entity.setHeSoCachDanh(bet.getPlayer().getHeSoCachDanh());

        // Thông tin số người chơi
        entity.setNgayChoi(bet.getNgay());
        entity.setCachDanh(bet.getCachDanh());
        entity.setSoDanh(bet.getSoDanh());
        entity.setMien(bet.getMien());

        // Kết quả theo đài
        entity.setTenDai(kq.getTenDai());
        entity.setTrung(kq.isTrung());
        entity.setGiaiTrung(serializeSafe(kq.getGiaiTrung())); // list -> JSON
        entity.setSoTrung(kq.getSoTrung());
        entity.setLyDo(kq.getLyDo());

        // Tiền thắng / thua
        entity.setTienTrung(kq.getTienTrung());
        entity.setTienTrungBaoLo(dto.getTienTrungBaoLo());
        entity.setTienTrungThuong(dto.getTienTrungThuong());
        entity.setTienTrungDacBiet(dto.getTienTrungDacBiet());
        entity.setTienDanh(parseTienDanhSafe(bet.getSoTien()));

        entity.setSourceSoId(bet.getId());     // NEW: gắn khoá nguồn
        entity.setSummary(false);             // NEW: bản chi tiết không phải summary

        return entity;
    }

    private KetQuaNguoiChoi buildEntityFromTongKet(
            Bet bet,
            DoiChieuKetQuaDto dto
    ) {
        KetQuaNguoiChoi entity = new KetQuaNguoiChoi();

        // Player info
        entity.setPlayerId(bet.getPlayer().getId());
        entity.setPlayerName(bet.getPlayer().getName());
        entity.setHoaHong(bet.getPlayer().getHoaHong());
        entity.setHeSoCachDanh(bet.getPlayer().getHeSoCachDanh());

        // Thông tin số người chơi
        entity.setNgayChoi(bet.getNgay());
        entity.setCachDanh(bet.getCachDanh());
        entity.setSoDanh(bet.getSoDanh());
        entity.setMien(bet.getMien());

        // Kết quả tổng hợp
        entity.setTenDai(dto.getTenDai());
        entity.setTrung(dto.isTrung());
        entity.setGiaiTrung(dto.getGiaiTrung() == null ? null : String.join(",", dto.getGiaiTrung()));
        entity.setSoTrung(null);

        // Lý do
        if (dto.getGhiChu() != null && !dto.getGhiChu().isBlank()) {
            entity.setLyDo(dto.getGhiChu());
        } else if (dto.getSaiLyDo() != null && !dto.getSaiLyDo().isEmpty()) {
            entity.setLyDo(String.join("; ", dto.getSaiLyDo()));
        } else {
            entity.setLyDo(null);
        }

        // Tiền thắng / thua
        entity.setTienDanh(parseTienDanhSafe(bet.getSoTien()));
        entity.setTienTrung(dto.getTienTrung());
        entity.setTienTrungBaoLo(dto.getTienTrungBaoLo());
        entity.setTienTrungThuong(dto.getTienTrungThuong());
        entity.setTienTrungDacBiet(dto.getTienTrungDacBiet());

        entity.setSourceSoId(bet.getId());  // NEW
        entity.setSummary(true);           // NEW: bản tổng hợp

        return entity;
    }

    // ===================== UTIL =====================

    private Double parseTienDanhSafe(String tienDanhStr) {
        if (tienDanhStr == null) return null;
        try {
            return Double.valueOf(tienDanhStr.replaceAll("[,\\s]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeSafe(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "));
        }
    }
}
