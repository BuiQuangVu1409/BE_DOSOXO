package com.example.doxoso.service;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
import com.example.doxoso.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TongTienTrungService {

    @Autowired
    private KetQuaNguoiChoiRepository ketQuaRepo;
    @Autowired
    private PlayerRepository playerRepo;
    /** Gom từ list DTO vừa dò (1 player) */
    public TongTienTrungDto tongHopTuKetQuaDtos(
            Long playerId, LocalDate ngay, List<DoiChieuKetQuaDto> ketQua) {

        // 1) Lọc các record trúng và có tiền > 0
        List<DoiChieuKetQuaDto> trungList = ketQua.stream()
                .filter(k -> Boolean.TRUE.equals(k.isTrung()))
                .filter(k -> safe(k.getTienTrung()).compareTo(BigDecimal.ZERO) > 0)
                .toList();

        // 2) Group theo Mã Miền (MB/MT/MN) → Đài → Cách đánh (đã chuẩn hoá)
        Map<String, Map<String, Map<String, BigDecimal>>> map =
                trungList.stream().collect(Collectors.groupingBy(
                        k -> toMienCode(k.getMien()),               // dùng mã miền để ổn định sort
                        Collectors.groupingBy(
                                k -> upperOrName(k.getTenDai()),
                                Collectors.groupingBy(
                                        k -> normalizeCachDanh(k.getCachDanh()), // <-- CHUẨN HOÁ
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                k -> safe(k.getTienTrung()),
                                                BigDecimal::add
                                        )
                                )
                        )
                ));

        // 3) Convert sang DTO cây + scale tiền
        TongTienTrungDto dto = new TongTienTrungDto();
        dto.setPlayerId(playerId);
        dto.setNgay(ngay.toString());

        List<TongTienTrungDto.MienDto> mienDtos = new ArrayList<>();
        BigDecimal tongAll = BigDecimal.ZERO;

        for (var eMien : map.entrySet()) {
            String mienCode = eMien.getKey();
            var daiMap = eMien.getValue();

            List<TongTienTrungDto.DaiDto> daiDtos = new ArrayList<>();
            BigDecimal tongMien = BigDecimal.ZERO;

            for (var eDai : daiMap.entrySet()) {
                String tenDai = eDai.getKey();
                var cachDanhMap = eDai.getValue();

                List<TongTienTrungDto.CachDanhDto> cdDtos = new ArrayList<>();
                BigDecimal tongDai = BigDecimal.ZERO;

                for (var eCd : cachDanhMap.entrySet()) {
                    String cd = eCd.getKey();
                    BigDecimal amount = eCd.getValue(); // tránh trùng tên method money(...)

                    TongTienTrungDto.CachDanhDto cdDto = new TongTienTrungDto.CachDanhDto();
                    cdDto.setCachDanh(cd);
                    cdDto.setTienTrung(money(amount));       // scale tiền khi xuất
                    cdDtos.add(cdDto);

                    tongDai = tongDai.add(amount);
                }

                TongTienTrungDto.DaiDto daiDto = new TongTienTrungDto.DaiDto();
                daiDto.setTenDai(tenDai);
                daiDto.setCacCachDanh(cdDtos.stream()
                        .sorted(Comparator.comparing(TongTienTrungDto.CachDanhDto::getCachDanh))
                        .toList());
                daiDto.setTongTienDai(money(tongDai));      // scale tiền

                daiDtos.add(daiDto);
                tongMien = tongMien.add(tongDai);
            }

            TongTienTrungDto.MienDto mienDto = new TongTienTrungDto.MienDto();
            // Hiển thị "MIỀN BẮC/TRUNG/NAM"
            mienDto.setMien(displayMien(mienCode));
            mienDto.setCacDai(daiDtos.stream()
                    .sorted(Comparator.comparing(TongTienTrungDto.DaiDto::getTenDai))
                    .toList());
            mienDto.setTongTienMien(money(tongMien));       // scale tiền

            mienDtos.add(mienDto);
            tongAll = tongAll.add(tongMien);
        }

        // 4) Sắp xếp miền MB → MT → MN
        dto.setCacMien(mienDtos.stream()
                .sorted(Comparator.comparingInt(TongTienTrungService::mienOrder))
                .toList());
        dto.setTongToanBo(money(tongAll));                  // scale tiền

        return dto;
    }

    /** Đọc từ DB (1 player) */
    public TongTienTrungDto tongHopTuDb(Long playerId, LocalDate ngay) {
        var list = ketQuaRepo.findByPlayerIdAndNgayChoi(playerId, ngay);

        List<DoiChieuKetQuaDto> tmp = list.stream()
                .filter(k -> Boolean.TRUE.equals(k.getTrung()))
                .map(k -> {
                    var d = new DoiChieuKetQuaDto();
                    d.setMien(k.getMien());
                    d.setTenDai(k.getTenDai());
                    d.setCachDanh(k.getCachDanh());
                    d.setTrung(true);
                    d.setTienTrung(k.getTienTrung() == null ? 0.0 : k.getTienTrung());
                    return d;
                })
                .toList();

        return tongHopTuKetQuaDtos(playerId, ngay, tmp);
    }

    /** Đọc từ DB gộp TẤT CẢ player trong ngày (có lọc trùng & chuẩn hoá cách đánh) */
    public TongTienTrungDto tongHopTatCaPlayerTuDb(LocalDate ngay) {
        var list = ketQuaRepo.findByNgayChoi(ngay); // cần repo này

        // --- CHỐNG TRÙNG: nếu DB có ghi đúp, ta lọc theo key định danh ---
        Set<String> seen = new HashSet<>();
        List<DoiChieuKetQuaDto> tmp = list.stream()
                .filter(k -> Boolean.TRUE.equals(k.getTrung()))
                .filter(k -> {
                    String key = String.join("|",
                            String.valueOf(k.getPlayerId()),
                            String.valueOf(k.getNgayChoi()),
                            toMienCode(k.getMien()),
                            upperOrName(k.getTenDai()),
                            normalizeCachDanh(k.getCachDanh()),
                            String.valueOf(k.getSoDanh()) // nếu entity có field này
                    );
                    return seen.add(key);
                })
                .map(k -> {
                    var d = new DoiChieuKetQuaDto();
                    d.setMien(k.getMien());
                    d.setTenDai(k.getTenDai());
                    d.setCachDanh(k.getCachDanh());
                    d.setTrung(true);
                    d.setTienTrung(k.getTienTrung() == null ? 0.0 : k.getTienTrung());
                    return d;
                })
                .toList();

        // playerId để null vì gộp tất cả
        TongTienTrungDto dto = tongHopTuKetQuaDtos(null, ngay, tmp);

        // Double-check scale (phòng sót)
        dto.setTongToanBo(money(dto.getTongToanBo()));
        dto.getCacMien().forEach(mien -> {
            mien.setTongTienMien(money(mien.getTongTienMien()));
            mien.getCacDai().forEach(dai -> {
                dai.setTongTienDai(money(dai.getTongTienDai()));
                dai.getCacCachDanh().forEach(cd -> cd.setTienTrung(money(cd.getTienTrung())));
            });
        });

        return dto;
    }
    /** Đọc DB và trả về KẾT QUẢ THEO TỪNG PLAYER + grand total */
    public TongTienTrungAllPlayersDto tongHopTheoTungPlayer(LocalDate ngay) {
        // Lấy tất cả record TRÚNG trong ngày
        var list = ketQuaRepo.findByNgayChoiAndTrungTrue(ngay);
        // group theo playerId
        Map<Long, List<com.example.doxoso.model.KetQuaNguoiChoi>> byPlayer =
                list.stream().collect(Collectors.groupingBy(com.example.doxoso.model.KetQuaNguoiChoi::getPlayerId));

        BigDecimal grand = BigDecimal.ZERO;
        List<TongTienTrungAllPlayersDto.PlayerBlock> blocks = new ArrayList<>();

        for (var e : byPlayer.entrySet()) {
            Long pid = e.getKey();
            var items = e.getValue();

            // map sang DoiChieuKetQuaDto để tái dùng aggregator hiện có
            List<DoiChieuKetQuaDto> tmp = items.stream().map(k -> {
                var d = new DoiChieuKetQuaDto();
                d.setMien(k.getMien());
                d.setTenDai(k.getTenDai());
                d.setCachDanh(k.getCachDanh());
                d.setTrung(true);
                d.setTienTrung(k.getTienTrung() == null ? 0.0 : k.getTienTrung());
                return d;
            }).toList();

            // tái dùng hàm tổng hợp “1 player”
            TongTienTrungDto perPlayer = tongHopTuKetQuaDtos(pid, ngay, tmp);

            // scale cho chắc
            perPlayer.setTongToanBo(money(perPlayer.getTongToanBo()));
            perPlayer.getCacMien().forEach(m -> {
                m.setTongTienMien(money(m.getTongTienMien()));
                m.getCacDai().forEach(d -> {
                    d.setTongTienDai(money(d.getTongTienDai()));
                    d.getCacCachDanh().forEach(cd -> cd.setTienTrung(money(cd.getTienTrung())));
                });
            });

            TongTienTrungAllPlayersDto.PlayerBlock block = new TongTienTrungAllPlayersDto.PlayerBlock();
            block.setPlayerId(pid);
            // Nếu bạn có tên trong entity: block.setPlayerName(items.get(0).getPlayerName());
            block.setTongToanBo(perPlayer.getTongToanBo());
            block.setCacMien(perPlayer.getCacMien());

            blocks.add(block);
            grand = grand.add(perPlayer.getTongToanBo());
        }

        // dựng output
        TongTienTrungAllPlayersDto out = new TongTienTrungAllPlayersDto();
        out.setNgay(ngay.toString());
        out.setGrandTotal(money(grand));
        // order players theo tổng giảm dần (tuỳ thích)
        out.setPlayers(blocks.stream()
                .sorted(Comparator.comparing(TongTienTrungAllPlayersDto.PlayerBlock::getTongToanBo).reversed())
                .toList());
        return out;
    }



    /** Tổng hợp THEO TỪNG PLAYER trong 1 ngày, có lọc miền (MB/MT/MN hoặc rỗng) + join tên player */
    public TongTienTrungAllPlayersDto tongHopTheoTungPlayerTheoMien(LocalDate ngay, String mienParam) {
        // 1) chuẩn hoá filter miền thành code {MB,MT,MN}
        Set<String> filterCodes = toMienCodesFromParam(mienParam); // null/empty => {MB,MT,MN}

        // 2) lấy dữ liệu đã TRÚNG trong ngày
        List<KetQuaNguoiChoi> raw;
        if (filterCodes.size() == 3) {
            raw = ketQuaRepo.findByNgayChoiAndTrungTrue(ngay);
        } else {
            // nếu DB lưu 'mien' là MB/MT/MN thì query thẳng; nếu lưu 'MIỀN ...', fallback lọc code ở code
            raw = ketQuaRepo.findByNgayChoiAndTrungTrueAndMienIn(ngay, filterCodes);
            if (raw.isEmpty()) {
                raw = ketQuaRepo.findByNgayChoiAndTrungTrue(ngay).stream()
                        .filter(k -> filterCodes.contains(toMienCode(k.getMien())))
                        .toList();
            }
        }

        // 3) group theo playerId
        Map<Long, List<KetQuaNguoiChoi>> byPlayer =
                raw.stream().collect(Collectors.groupingBy(KetQuaNguoiChoi::getPlayerId));

        // 4) join tên player một lượt
        Map<Long, String> id2name = playerRepo.findByIdIn(byPlayer.keySet())
                .stream().collect(Collectors.toMap(Player::getId, Player::getName));

        // 5) duyệt từng player, tái dùng aggregator 1-player
        BigDecimal grand = BigDecimal.ZERO;
        List<TongTienTrungAllPlayersDto.PlayerBlock> blocks = new ArrayList<>();

        for (var e : byPlayer.entrySet()) {
            Long pid = e.getKey();
            var items = e.getValue();

            // lọc theo miền code, map sang DoiChieuKetQuaDto
            List<DoiChieuKetQuaDto> tmp = items.stream()
                    .filter(k -> filterCodes.contains(toMienCode(k.getMien())))
                    .map(k -> {
                        var d = new DoiChieuKetQuaDto();
                        d.setMien(k.getMien());
                        d.setTenDai(k.getTenDai());
                        d.setCachDanh(k.getCachDanh());
                        d.setTrung(true);
                        d.setTienTrung(k.getTienTrung() == null ? 0.0 : k.getTienTrung());
                        return d;
                    })
                    .toList();

            if (tmp.isEmpty()) continue;

            // dựng cây miền->đài->cách đánh cho player này (tận dụng hàm bạn đã có)
            TongTienTrungDto perPlayer = tongHopTuKetQuaDtos(pid, ngay, tmp);

            // scale & đóng gói
            perPlayer.setTongToanBo(money(perPlayer.getTongToanBo()));
            perPlayer.getCacMien().forEach(m -> {
                m.setTongTienMien(money(m.getTongTienMien()));
                m.getCacDai().forEach(d -> {
                    d.setTongTienDai(money(d.getTongTienDai()));
                    d.getCacCachDanh().forEach(cd -> cd.setTienTrung(money(cd.getTienTrung())));
                });
            });

            var block = new TongTienTrungAllPlayersDto.PlayerBlock();
            block.setPlayerId(pid);
            block.setPlayerName(id2name.get(pid)); // tên player
            block.setTongToanBo(perPlayer.getTongToanBo());
            block.setCacMien(perPlayer.getCacMien());

            blocks.add(block);
            grand = grand.add(perPlayer.getTongToanBo());
        }

        // 6) output
        var out = new TongTienTrungAllPlayersDto();
        out.setNgay(ngay.toString());
        out.setGrandTotal(money(grand));
        out.setPlayers(blocks.stream()
                .sorted(Comparator.comparing(TongTienTrungAllPlayersDto.PlayerBlock::getTongToanBo).reversed())
                .toList());
        return out;
    }

    /** parse param 'mien' -> set code {MB,MT,MN} */
    private static Set<String> toMienCodesFromParam(String mienParam) {
        if (mienParam == null || mienParam.isBlank()) return Set.of("MB","MT","MN");
        String[] parts = mienParam.split("[,|]");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            String code = toMienCode(p);
            if ("MB".equals(code) || "MT".equals(code) || "MN".equals(code)) out.add(code);
        }
        return out.isEmpty() ? Set.of("MB","MT","MN") : out;
    }


    // =================== Helpers ===================

    private static int mienOrder(TongTienTrungDto.MienDto m) {
        String code = toMienCode(m.getMien()); // chấp cả khi m.getMien() là tên hiển thị
        return switch (code) {
            case "MB" -> 0;
            case "MT" -> 1;
            case "MN" -> 2;
            default -> 99;
        };
    }

    /** Scale tiền chuẩn (0 số lẻ; đổi 2 nếu muốn) */
    private static BigDecimal money(BigDecimal b) { return safe(b).setScale(0, BigDecimal.ROUND_HALF_UP); }

    private static BigDecimal safe(Double d) {
        return d == null ? BigDecimal.ZERO : BigDecimal.valueOf(d); // KHÔNG dùng new BigDecimal(double)
    }
    private static BigDecimal safe(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    private static String upper(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
    private static String upperOrName(String s) {
        return s == null ? "N/A" : s.trim().toUpperCase();
    }

    /** Chuẩn hoá cách đánh: bỏ dấu câu, rút gọn space, map biến thể thường gặp */
    private static String normalizeCachDanh(String s) {
        if (s == null) return "";
        String u = s.trim().toUpperCase();
        // bỏ ký tự không phải chữ/số/khoảng trắng (loại dấu chấm, phẩy...)
        u = u.replaceAll("[^A-Z0-9À-Ỵ\\s]", "");
        // rút gọn space
        u = u.replaceAll("\\s+", " ");
        // map biến thể
        u = u.replaceAll("\\bXUYEN\\b", "XUYÊN");
        u = u.replaceAll("\\bCHAN\\b", "CHÂN");
        return u;
    }

    /** Map các biến thể tên miền → mã miền ổn định */
    private static String toMienCode(String mien) {
        String u = upper(mien);
        if (u.equals("MB") || u.contains("BẮC") || u.contains("BAC"))   return "MB";
        if (u.equals("MT") || u.contains("TRUNG"))                      return "MT";
        if (u.equals("MN") || u.contains("NAM"))                        return "MN";
        return "??";
    }

    /** Hiển thị tên miền đẹp từ mã miền */
    private static String displayMien(String mien) {
        String c = toMienCode(mien);
        return switch (c) {
            case "MB" -> "MIỀN BẮC";
            case "MT" -> "MIỀN TRUNG";
            case "MN" -> "MIỀN NAM";
            default -> upper(mien);
        };
    }
}
