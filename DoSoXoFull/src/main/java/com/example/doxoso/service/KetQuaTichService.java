//package com.example.doxoso.service;
//
//import com.example.doxoso.model.*;
//import com.example.doxoso.repository.BetRepository;
//import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
//import com.example.doxoso.repository.KetQuaTichRepository;
//import com.example.doxoso.repository.PlayerRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.text.Normalizer;
//import java.time.LocalDate;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class KetQuaTichService {
//
//    private final KetQuaTichRepository ketQuaTichRepo;
//    private final BetRepository betRepository;
//    private final PlayerRepository playerRepository;
//    private final KetQuaNguoiChoiRepository ketQuaNguoiChoiRepo;
//
//    private final TongTienTrungService tongTienTrungService;              // hiện giờ ít dùng, giữ lại để sau
//    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;
//    private final TongTienAnThuaMienService tongTienAnThuaMienService;    // hiện chưa dùng, để sẵn
//    private final LichQuayXoSoService lichQuayXoSoService;
//    private final KetQuaService ketQuaService;
//
//    // 👉 Tổng tiền đánh theo miền (KHÔNG LỚN/NHỎ, đã nhân số đài)
//    private final TongTienDanhTheoMienService tongTienDanhTheoMienService;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    // ===== normalize / alias =====
//    private static final Map<String, String> ALIASES = Map.ofEntries(
//            Map.entry("TP.HO CHI MINH", "TP HO CHI MINH"),
//            Map.entry("TP HO CHI MINH", "TP HO CHI MINH"),
//            Map.entry("TP HCM", "TP HO CHI MINH"),
//            Map.entry("HCM", "TP HO CHI MINH"),
//            Map.entry("HO CHI MINH", "TP HO CHI MINH"),
//            Map.entry("HO CHI MINH CITY", "TP HO CHI MINH"),
//            Map.entry("BA RIA VUNG TAU", "VUNG TAU"),
//            Map.entry("BRVT", "VUNG TAU"),
//            Map.entry("TAYNINH", "TAY NINH"),
//            Map.entry("BINHDUONG", "BINH DUONG"),
//            Map.entry("BINHPHUOC", "BINH PHUOC"),
//            Map.entry("DA LAT", "DA LAT"),
//            Map.entry("CAN THO", "CAN THO"),
//            Map.entry("TP.CAN THO", "CAN THO")
//    );
//
//    private static String normalizeNoAccent(String s) {
//        if (s == null) return "";
//
//        String noAccent = Normalizer.normalize(s, Normalizer.Form.NFD)
//                .replaceAll("\\p{M}", "");
//        noAccent = noAccent
//                .replace('đ', 'd')
//                .replace('Đ', 'D');
//
//        return noAccent
//                .toUpperCase()
//                .trim()
//                .replaceAll("[\\-_.]+", " ")
//                .replaceAll("\\s+", " ");
//    }
//
//    private static String canonicalProvince(String raw) {
//        String u = normalizeNoAccent(raw);
//        String canon = ALIASES.getOrDefault(u, u);
//        canon = canon.replaceFirst("^TP\\s+", "").trim();
//        return canon;
//    }
//
//    private Map<String, Set<String>> scheduleSets(LocalDate ngay) {
//        Map<String, Set<String>> out = new HashMap<>();
//        out.put("MB", new HashSet<>());
//        out.put("MT", new HashSet<>());
//        out.put("MN", new HashSet<>());
//
//        LichQuayXoSo lich = lichQuayXoSoService.traCuuTheoNgay(ngay);
//        Map<String, List<String>> m = lich.getKetQua();
//
//        for (String key : List.of("MIỀN BẮC", "MIỀN TRUNG", "MIỀN NAM")) {
//            List<String> list = m.getOrDefault(key, List.of());
//            Set<String> target = switch (key) {
//                case "MIỀN BẮC" -> out.get("MB");
//                case "MIỀN TRUNG" -> out.get("MT");
//                case "MIỀN NAM" -> out.get("MN");
//                default -> new HashSet<>();
//            };
//            for (String province : list) {
//                target.add(canonicalProvince(province));
//            }
//        }
//        return out;
//    }
//
//    private String toCode(String raw, Map<String, Set<String>> sets) {
//        String u = normalizeNoAccent(raw);
//        if (u.isEmpty()) return "";
//        if (u.startsWith("MB") || u.contains("MIEN BAC")) return "MB";
//        if (u.startsWith("MT") || u.contains("MIEN TRUNG")) return "MT";
//        if (u.startsWith("MN") || u.contains("MIEN NAM")) return "MN";
//
//        String token = canonicalProvince(u);
//        for (String key : List.of("MB", "MT", "MN")) {
//            if (sets.get(key).contains(token)) return key;
//        }
//        return u;
//    }
//
//    private static String display(String code) {
//        return switch (code) {
//            case "MB" -> "MIỀN BẮC";
//            case "MT" -> "MIỀN TRUNG";
//            case "MN" -> "MIỀN NAM";
//            default -> code;
//        };
//    }
//
//    // ===== DTO nhỏ để nhét vào chiTietTrung =====
//    public static class WinDetail {
//        public String dai;
//        public String cachDanh;
//        public String soDanh;
//        public String giai;
//        public Double tienTrung;
//
//        public WinDetail(String dai, String cachDanh, String soDanh, String giai, Double tienTrung) {
//            this.dai = dai;
//            this.cachDanh = cachDanh;
//            this.soDanh = soDanh;
//            this.giai = giai;
//            this.tienTrung = tienTrung;
//        }
//    }
//
//    // ==================== FIX CHẴN/LẺ: không bắt nhầm 2 CHÂN / 3 CHÂN ======================
//    // normalize:
//    //  - "CHẴN ĐẦU" -> "CHAN DAU"
//    //  - "2 chân"   -> "2 CHAN"
//    private static boolean isHaiBaChanNorm(String cachNorm) {
//        return cachNorm != null && cachNorm.matches("^(2|3)\\s*CHAN(\\s|$).*");
//    }
//
//    private static boolean isChanKeoNorm(String cachNorm) {
//        if (cachNorm == null || cachNorm.isBlank()) return false;
//        // chỉ bắt "CHAN ..." ở ĐẦU chuỗi, và loại 2/3 CHÂN
//        return cachNorm.matches("^CHAN(\\s|$).*") && !isHaiBaChanNorm(cachNorm);
//    }
//
//    private static boolean isLeKeoNorm(String cachNorm) {
//        if (cachNorm == null || cachNorm.isBlank()) return false;
//        // chỉ bắt "LE ..." ở ĐẦU chuỗi
//        return cachNorm.matches("^LE(\\s|$).*");
//    }
//
//    // =======================================================================
//    //  CHÍNH: chạy & lưu kết quả tịch cho 1 người chơi
//    // =======================================================================
////    @Transactional
////    public List<KetQuaTich> runAndSaveForPlayer(Long playerId, String playerName, LocalDate ngay) {
////
////        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);
////
////        ketQuaNguoiChoiRepo.deleteByPlayerIdAndNgayChoi(playerId, ngay);
////
////        if (soList.isEmpty()) {
////            ketQuaTichRepo.deleteByPlayerIdAndNgay(playerId, ngay);
////            return List.of();
////        }
////
////        ketQuaService.doKetQua(soList);
////
////        Map<String, Set<String>> sets = scheduleSets(ngay);
////
////        TongHopHoaHongLonNhoDto hhln =
////                tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);
////
////        // 3) TIỀN ĐÁNH theo nhóm (từ BET) – chỉ dùng hiển thị @Transient
////        Map<String, BigDecimal> tienLonByCode = new HashMap<>();
////        Map<String, BigDecimal> tienNhoByCode = new HashMap<>();
////
////        Map<String, BigDecimal> tienChanByCode = new HashMap<>();
////        Map<String, BigDecimal> tienLeByCode = new HashMap<>();
////
////        Map<String, Set<String>> lonTypesByCode = new HashMap<>();
////        Map<String, Set<String>> nhoTypesByCode = new HashMap<>();
////
////        Map<String, Set<String>> chanTypesByCode = new HashMap<>();
////        Map<String, Set<String>> leTypesByCode = new HashMap<>();
////
////        for (Bet so : soList) {
////            BigDecimal stake = parseTienDanh(so.getSoTien());
////            String code = toCode(so.getMien(), sets);
////
////            if (!"MB".equals(code) && !"MT".equals(code) && !"MN".equals(code)) continue;
////
////            String cach = normalizeNoAccent(so.getCachDanh());
////
////            boolean isLon = cach.contains("LON");
////            boolean isNho = cach.contains("NHO");
////
////            if (isLon) {
////                tienLonByCode.merge(code, stake, BigDecimal::add);
////
////                String label = "LỚN";
////                if (cach.contains("DAU")) label = "LỚN ĐẦU";
////                else if (cach.contains("DUOI")) label = "LỚN ĐUÔI";
////
////                lonTypesByCode.computeIfAbsent(code, c -> new HashSet<>()).add(label);
////            }
////
////            if (isNho) {
////                tienNhoByCode.merge(code, stake, BigDecimal::add);
////
////                String label = "NHỎ";
////                if (cach.contains("DAU")) label = "NHỎ ĐẦU";
////                else if (cach.contains("DUOI")) label = "NHỎ ĐUÔI";
////
////                nhoTypesByCode.computeIfAbsent(code, c -> new HashSet<>()).add(label);
////            }
////
////            // ✅ CHẴN / LẺ stake: dùng hàm FIX (không bắt nhầm 2/3 CHÂN)
////            boolean isChan = isChanKeoNorm(cach);
////            boolean isLe = isLeKeoNorm(cach);
////
////            if (isChan) {
////                tienChanByCode.merge(code, stake, BigDecimal::add);
////
////                String label = "CHẴN";
////                if (cach.contains("DAU")) label = "CHẴN ĐẦU";
////                else if (cach.contains("DUOI")) label = "CHẴN ĐUÔI";
////
////                chanTypesByCode.computeIfAbsent(code, c -> new HashSet<>()).add(label);
////            }
////
////            if (isLe) {
////                tienLeByCode.merge(code, stake, BigDecimal::add);
////
////                String label = "LẺ";
////                if (cach.contains("DAU")) label = "LẺ ĐẦU";
////                else if (cach.contains("DUOI")) label = "LẺ ĐUÔI";
////
////                leTypesByCode.computeIfAbsent(code, c -> new HashSet<>()).add(label);
////            }
////        }
////
////        // 4) Tiền đánh thường theo miền
////        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
////        List<PlayerTongTienDanhTheoMienDto> tongList =
////                tongTienDanhTheoMienService.tinhTongTheoMienTheoNgay(playerId, ngay, ngay);
////
////        if (!tongList.isEmpty()) {
////            PlayerTongTienDanhTheoMienDto dto = tongList.get(0);
////            tienDanhByCode.put("MB", Optional.ofNullable(dto.getMienBac()).orElse(BigDecimal.ZERO));
////            tienDanhByCode.put("MT", Optional.ofNullable(dto.getMienTrung()).orElse(BigDecimal.ZERO));
////            tienDanhByCode.put("MN", Optional.ofNullable(dto.getMienNam()).orElse(BigDecimal.ZERO));
////        }
////
////        String resolvedName = resolvePlayerName(playerId, playerName, hhln, soList);
////
////        Double hoaHongPlayer = playerRepository.findById(playerId)
////                .map(Player::getHoaHong)
////                .orElse(null);
////
////        List<KetQuaTich> existedRows = ketQuaTichRepo.findByPlayerIdAndNgay(playerId, ngay);
////        Map<String, KetQuaTich> existedByCode = new HashMap<>();
////        for (KetQuaTich r : existedRows) {
////            if (r.getMienCode() != null) existedByCode.put(r.getMienCode(), r);
////        }
////
////        List<KetQuaNguoiChoi> ketQuaTrongNgay =
////                ketQuaNguoiChoiRepo.findByPlayerIdAndNgayChoi(playerId, ngay);
////
////        Map<String, BigDecimal> tienTrungThuongByCode = new HashMap<>();
////
////        Map<String, BigDecimal> tienLonNetByCode = new HashMap<>();
////        Map<String, BigDecimal> tienNhoNetByCode = new HashMap<>();
////
////        Map<String, BigDecimal> tienChanNetByCode = new HashMap<>();
////        Map<String, BigDecimal> tienLeNetByCode = new HashMap<>();
////
////        if (ketQuaTrongNgay != null) {
////            for (KetQuaNguoiChoi k : ketQuaTrongNgay) {
////                String codeOfRow = toCode(k.getMien(), sets);
////                if (!"MB".equals(codeOfRow) && !"MT".equals(codeOfRow) && !"MN".equals(codeOfRow)) continue;
////
////                String cachNorm = normalizeNoAccent(k.getCachDanh());
////
////                boolean laLon = cachNorm.contains("LON");
////                boolean laNho = cachNorm.contains("NHO");
////                boolean laLonNho = laLon || laNho;
////
////                // ✅ FIX: CHẴN/LẺ chỉ match đúng kèo CHẴN/LẺ, không match 2/3 CHÂN
////                boolean laChan = isChanKeoNorm(cachNorm);
////                boolean laLe = isLeKeoNorm(cachNorm);
////                boolean laChanLe = laChan || laLe;
////
////                BigDecimal tienTrung = bd(k.getTienTrung());
////                BigDecimal tienDanh = bd(k.getTienDanh());
////
////                // 1) KÈO THƯỜNG (bao gồm 2 CHÂN / 3 CHÂN vì laChanLe=false)
////                if (!laLonNho && !laChanLe) {
////                    if (Boolean.TRUE.equals(k.getTrung()) && tienTrung.compareTo(BigDecimal.ZERO) > 0) {
////                        tienTrungThuongByCode.merge(codeOfRow, tienTrung, BigDecimal::add);
////                    }
////                    continue;
////                }
////
////                // delta: trúng => +tienTrung, trật => -tienDanh
////                BigDecimal delta;
////                if (Boolean.TRUE.equals(k.getTrung()) && tienTrung.compareTo(BigDecimal.ZERO) > 0) {
////                    delta = tienTrung;
////                } else {
////                    delta = tienDanh.negate();
////                }
////
////                // 2) LỚN / NHỎ
////                if (laLonNho) {
////                    if (laLon) tienLonNetByCode.merge(codeOfRow, delta, BigDecimal::add);
////                    if (laNho) tienNhoNetByCode.merge(codeOfRow, delta, BigDecimal::add);
////                    continue;
////                }
////
////                // 3) CHẴN / LẺ
////                if (laChanLe) {
////                    if (laChan) tienChanNetByCode.merge(codeOfRow, delta, BigDecimal::add);
////                    if (laLe) tienLeNetByCode.merge(codeOfRow, delta, BigDecimal::add);
////
////                    if (laChan) {
////                        String label = "CHẴN";
////                        if (cachNorm.contains("DAU")) label = "CHẴN ĐẦU";
////                        else if (cachNorm.contains("DUOI")) label = "CHẴN ĐUÔI";
////                        chanTypesByCode.computeIfAbsent(codeOfRow, c -> new HashSet<>()).add(label);
////                    }
////                    if (laLe) {
////                        String label = "LẺ";
////                        if (cachNorm.contains("DAU")) label = "LẺ ĐẦU";
////                        else if (cachNorm.contains("DUOI")) label = "LẺ ĐUÔI";
////                        leTypesByCode.computeIfAbsent(codeOfRow, c -> new HashSet<>()).add(label);
////                    }
////                }
////            }
////        }
////
////        List<KetQuaTich> rows = new ArrayList<>();
////
////        for (String code : new String[]{"MB", "MT", "MN"}) {
////            String disp = display(code);
////
////            BigDecimal tienTrungThuong = tienTrungThuongByCode.getOrDefault(code, BigDecimal.ZERO);
////            BigDecimal tienDanh = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);
////
////            BigDecimal tienLonDanh = tienLonByCode.getOrDefault(code, BigDecimal.ZERO);
////            BigDecimal tienNhoDanh = tienNhoByCode.getOrDefault(code, BigDecimal.ZERO);
////
////            BigDecimal tienChanDanh = tienChanByCode.getOrDefault(code, BigDecimal.ZERO);
////            BigDecimal tienLeDanh = tienLeByCode.getOrDefault(code, BigDecimal.ZERO);
////
////            BigDecimal tienLonNet = tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
////            BigDecimal tienNhoNet = tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
////
////            BigDecimal tienChanNet = tienChanNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
////            BigDecimal tienLeNet = tienLeNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
////
////            BigDecimal tienLonNhoNet = tienLonNet.add(tienNhoNet);
////            BigDecimal tienChanLeNet = tienChanNet.add(tienLeNet);
////
////            BigDecimal tienHH = BigDecimal.ZERO;
////            if (hoaHongPlayer != null) {
////                BigDecimal rate = bd(hoaHongPlayer).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);
////                tienHH = tienDanh.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP);
////            }
////
////            BigDecimal tienAT = tienTrungThuong.subtract(tienHH).setScale(2, BigDecimal.ROUND_HALF_UP);
////
////            String lonLabel = buildLonLabel(lonTypesByCode.get(code));
////            String nhoLabel = buildNhoLabel(nhoTypesByCode.get(code));
////
////            String chanLabel = buildChanLabel(chanTypesByCode.get(code));
////            String leLabel = buildLeLabel(leTypesByCode.get(code));
////
////            // ✅ chiTietTrung: chỉ kèo thường, bỏ L/N & CHẴN/LẺ (nhưng KHÔNG bỏ 2/3 CHÂN)
////            String jsonChiTiet = buildChiTietJsonForRegion(ketQuaTrongNgay, code, sets);
////
////            KetQuaTich entity = KetQuaTich.builder()
////                    .playerId(playerId)
////                    .playerName(resolvedName)
////                    .ngay(ngay)
////                    .mienCode(code)
////                    .mienDisplay(disp)
////
////                    .tienTrung(tienTrungThuong)
////                    .tienHoaHong(tienHH)
////                    .tienLonNho(tienLonNhoNet)
////                    .tienAnThua(tienAT)
////
////                    .tienDanh(tienDanh)
////                    .tienDanhDaNhanHoaHong(tienHH)
////                    .tienDanhDaNhanHoaHongCongLonNho(tienHH.add(tienLonNhoNet))
////                    .chiTietTrung(jsonChiTiet)
////
////                    // L/N
////                    .tienLonDanh(tienLonDanh)
////                    .tienNhoDanh(tienNhoDanh)
////                    .tienLonNet(tienLonNet)
////                    .tienNhoNet(tienNhoNet)
////                    .lonLabel(lonLabel)
////                    .nhoLabel(nhoLabel)
////
////                    // CHẴN/LẺ
////                    .tienChanDanh(tienChanDanh)
////                    .tienLeDanh(tienLeDanh)
////                    .tienChanNet(tienChanNet)
////                    .tienLeNet(tienLeNet)
////                    .tienChanLe(tienChanLeNet)
////                    .chanLabel(chanLabel)
////                    .leLabel(leLabel)
////
////                    .build();
////
////            KetQuaTich old = existedByCode.get(code);
////            if (old != null) {
////                entity.setId(old.getId());
////                entity.setVersion(old.getVersion());
////                entity.setCreatedAt(old.getCreatedAt());
////            }
////
////            rows.add(entity);
////        }
////
////        List<KetQuaTich> saved = ketQuaTichRepo.saveAll(rows);
////
////        for (KetQuaTich kq : saved) {
////            kq.setHoaHongPlayer(hoaHongPlayer);
////
////            String code = kq.getMienCode();
////            if (code == null) continue;
////
////            kq.setTienLonDanh(tienLonByCode.getOrDefault(code, BigDecimal.ZERO));
////            kq.setTienNhoDanh(tienNhoByCode.getOrDefault(code, BigDecimal.ZERO));
////
////            kq.setTienLonNet(tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP));
////            kq.setTienNhoNet(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP));
////
////            kq.setLonLabel(buildLonLabel(lonTypesByCode.get(code)));
////            kq.setNhoLabel(buildNhoLabel(nhoTypesByCode.get(code)));
////
////            kq.setTienChanDanh(tienChanByCode.getOrDefault(code, BigDecimal.ZERO));
////            kq.setTienLeDanh(tienLeByCode.getOrDefault(code, BigDecimal.ZERO));
////
////            BigDecimal cn = tienChanNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
////            BigDecimal ln = tienLeNetByCode.getOrDefault(code, BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
////
////            kq.setTienChanNet(cn);
////            kq.setTienLeNet(ln);
////            kq.setTienChanLe(cn.add(ln));
////
////            kq.setChanLabel(buildChanLabel(chanTypesByCode.get(code)));
////            kq.setLeLabel(buildLeLabel(leTypesByCode.get(code)));
////        }
////
////        return saved;
////    }
//    @Transactional
//    public List<KetQuaTich> runAndSaveForPlayer(Long playerId, String playerName, LocalDate ngay) {
//
//        // =========================
//        // 1️⃣ LẤY BET MỚI NHẤT
//        // =========================
//        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);
//
//        // =========================
//        // 2️⃣ XOÁ TOÀN BỘ SNAPSHOT & CHI TIẾT CŨ
//        // =========================
//        ketQuaTichRepo.deleteByPlayerIdAndNgay(playerId, ngay);
//        ketQuaNguoiChoiRepo.deleteByPlayerIdAndNgayChoi(playerId, ngay);
//
//        // Không còn bet → không tạo snapshot
//        if (soList.isEmpty()) {
//            return List.of();
//        }
//
//        // =========================
//        // 3️⃣ TÍNH LẠI KẾT QUẢ
//        // =========================
//        ketQuaService.doKetQua(soList);
//
//        Map<String, Set<String>> sets = scheduleSets(ngay);
//
//        TongHopHoaHongLonNhoDto hhln =
//                tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);
//
//        // =========================
//        // 4️⃣ TIỀN ĐÁNH THEO BET
//        // =========================
//        Map<String, BigDecimal> tienLonByCode = new HashMap<>();
//        Map<String, BigDecimal> tienNhoByCode = new HashMap<>();
//        Map<String, BigDecimal> tienChanByCode = new HashMap<>();
//        Map<String, BigDecimal> tienLeByCode = new HashMap<>();
//
//        Map<String, Set<String>> lonTypesByCode = new HashMap<>();
//        Map<String, Set<String>> nhoTypesByCode = new HashMap<>();
//        Map<String, Set<String>> chanTypesByCode = new HashMap<>();
//        Map<String, Set<String>> leTypesByCode = new HashMap<>();
//
//        for (Bet so : soList) {
//            BigDecimal stake = parseTienDanh(so.getSoTien());
//            String code = toCode(so.getMien(), sets);
//            if (!Set.of("MB", "MT", "MN").contains(code)) continue;
//
//            String cach = normalizeNoAccent(so.getCachDanh());
//
//            if (cach.contains("LON")) {
//                tienLonByCode.merge(code, stake, BigDecimal::add);
//                lonTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
//                        .add(cach.contains("DAU") ? "LỚN ĐẦU" :
//                                cach.contains("DUOI") ? "LỚN ĐUÔI" : "LỚN");
//            }
//
//            if (cach.contains("NHO")) {
//                tienNhoByCode.merge(code, stake, BigDecimal::add);
//                nhoTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
//                        .add(cach.contains("DAU") ? "NHỎ ĐẦU" :
//                                cach.contains("DUOI") ? "NHỎ ĐUÔI" : "NHỎ");
//            }
//
//            if (isChanKeoNorm(cach)) {
//                tienChanByCode.merge(code, stake, BigDecimal::add);
//                chanTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
//                        .add(cach.contains("DAU") ? "CHẴN ĐẦU" :
//                                cach.contains("DUOI") ? "CHẴN ĐUÔI" : "CHẴN");
//            }
//
//            if (isLeKeoNorm(cach)) {
//                tienLeByCode.merge(code, stake, BigDecimal::add);
//                leTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
//                        .add(cach.contains("DAU") ? "LẺ ĐẦU" :
//                                cach.contains("DUOI") ? "LẺ ĐUÔI" : "LẺ");
//            }
//        }
//
//        // =========================
//        // 5️⃣ TIỀN ĐÁNH THƯỜNG
//        // =========================
//        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
//        tongTienDanhTheoMienService
//                .tinhTongTheoMienTheoNgay(playerId, ngay, ngay)
//                .stream().findFirst().ifPresent(dto -> {
//            tienDanhByCode.put("MB", bd(dto.getMienBac()));
//            tienDanhByCode.put("MT", bd(dto.getMienTrung()));
//            tienDanhByCode.put("MN", bd(dto.getMienNam()));
//        });
//
//        Double hoaHongPlayer = playerRepository.findById(playerId)
//                .map(Player::getHoaHong).orElse(null);
//
//        List<KetQuaNguoiChoi> ketQuaTrongNgay =
//                ketQuaNguoiChoiRepo.findByPlayerIdAndNgayChoi(playerId, ngay);
//
//        Map<String, BigDecimal> tienTrungThuongByCode = new HashMap<>();
//        Map<String, BigDecimal> tienLonNetByCode = new HashMap<>();
//        Map<String, BigDecimal> tienNhoNetByCode = new HashMap<>();
//        Map<String, BigDecimal> tienChanNetByCode = new HashMap<>();
//        Map<String, BigDecimal> tienLeNetByCode = new HashMap<>();
//
////        for (KetQuaNguoiChoi k : ketQuaTrongNgay) {
////            String code = toCode(k.getMien(), sets);
////            if (!Set.of("MB", "MT", "MN").contains(code)) continue;
////
////            String cach = normalizeNoAccent(k.getCachDanh());
////            BigDecimal tienTrung = bd(k.getTienTrung());
////            BigDecimal tienDanh = bd(k.getTienDanh());
////
////            boolean lon = cach.contains("LON");
////            boolean nho = cach.contains("NHO");
////            boolean chan = isChanKeoNorm(cach);
////            boolean le = isLeKeoNorm(cach);
////
////            if (!lon && !nho && !chan && !le) {
////                if (Boolean.TRUE.equals(k.getTrung()))
////                    tienTrungThuongByCode.merge(code, tienTrung, BigDecimal::add);
////                continue;
////            }
////
////            BigDecimal delta = Boolean.TRUE.equals(k.getTrung())
////                    ? tienTrung : tienDanh.negate();
////
////            if (lon) tienLonNetByCode.merge(code, delta, BigDecimal::add);
////            if (nho) tienNhoNetByCode.merge(code, delta, BigDecimal::add);
////            if (chan) tienChanNetByCode.merge(code, delta, BigDecimal::add);
////            if (le) tienLeNetByCode.merge(code, delta, BigDecimal::add);
////        }
//        for (KetQuaNguoiChoi k : ketQuaTrongNgay) {
//            String code = toCode(k.getMien(), sets);
//            if (!Set.of("MB", "MT", "MN").contains(code)) continue;
//
//            String cach = normalizeNoAccent(k.getCachDanh());
//
//            boolean lon = cach.contains("LON");
//            boolean nho = cach.contains("NHO");
//            boolean chan = isChanKeoNorm(cach);
//            boolean le = isLeKeoNorm(cach);
//
//            BigDecimal tienTrung = bd(k.getTienTrung());
//
//            // =========================
//            // KÈO THƯỜNG
//            // =========================
//            if (!lon && !nho && !chan && !le) {
//                if (Boolean.TRUE.equals(k.getTrung())) {
//                    tienTrungThuongByCode.merge(code, tienTrung, BigDecimal::add);
//                }
//                continue;
//            }
//
//            // =========================
//            // LỚN / NHỎ / CHẴN / LẺ
//            // =========================
//
//            // ⚠️ stake GỐC – KHÔNG nhân số đài
//            BigDecimal stakeMotDai = parseTienDanh(k.getTienDanh() + "");
//            int soDai = tinhSoDai(k);
//
//            BigDecimal stakeTong = stakeMotDai.multiply(BigDecimal.valueOf(soDai));
//
//
//            BigDecimal delta;
//            if (Boolean.TRUE.equals(k.getTrung())) {
//                delta = tienTrung;          // tiền trúng đã là tổng
//            } else {
//                delta = stakeTong.negate(); // ❗ PHẢI TRỪ TỔNG
//            }
//
//            if (lon) tienLonNetByCode.merge(code, delta, BigDecimal::add);
//            if (nho) tienNhoNetByCode.merge(code, delta, BigDecimal::add);
//            if (chan) tienChanNetByCode.merge(code, delta, BigDecimal::add);
//            if (le) tienLeNetByCode.merge(code, delta, BigDecimal::add);
//        }
//
//            // =========================
//        // 6️⃣ BUILD SNAPSHOT & INSERT
//        // =========================
//        List<KetQuaTich> rows = new ArrayList<>();
//
//        for (String code : List.of("MB", "MT", "MN")) {
//            BigDecimal tienDanh = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);
//
//            BigDecimal tienHH = hoaHongPlayer == null ? BigDecimal.ZERO :
//                    tienDanh.multiply(bd(hoaHongPlayer).divide(BigDecimal.valueOf(100)))
//                            .setScale(2, BigDecimal.ROUND_HALF_UP);
//
//            KetQuaTich kq = KetQuaTich.builder()
//                    .playerId(playerId)
//                    .playerName(resolvePlayerName(playerId, playerName, hhln, soList))
//                    .ngay(ngay)
//                    .mienCode(code)
//                    .mienDisplay(display(code))
//
//                    .tienTrung(tienTrungThuongByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienHoaHong(tienHH)
//                    .tienLonNho(
//                            tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO)
//                                    .add(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                                    .setScale(2, BigDecimal.ROUND_HALF_UP)
//                    )
//                    .tienAnThua(
//                            tienTrungThuongByCode.getOrDefault(code, BigDecimal.ZERO)
//                                    .subtract(tienHH).setScale(2, BigDecimal.ROUND_HALF_UP)
//                    )
//
//                    .tienDanh(tienDanh)
//                    .tienDanhDaNhanHoaHong(tienHH)
//                    .tienDanhDaNhanHoaHongCongLonNho(
//                            tienHH.add(
//                                    tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO)
//                                            .add(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                            )
//                    )
//
//                    // LỚN / NHỎ
//                    .tienLonDanh(tienLonByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienNhoDanh(tienNhoByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienLonNet(tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienNhoNet(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .lonLabel(buildLonLabel(lonTypesByCode.get(code)))
//                    .nhoLabel(buildNhoLabel(nhoTypesByCode.get(code)))
//
//                    // CHẴN / LẺ
//                    .tienChanDanh(tienChanByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienLeDanh(tienLeByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienChanNet(tienChanNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienLeNet(tienLeNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                    .tienChanLe(
//                            tienChanNetByCode.getOrDefault(code, BigDecimal.ZERO)
//                                    .add(tienLeNetByCode.getOrDefault(code, BigDecimal.ZERO))
//                    )
//                    .chanLabel(buildChanLabel(chanTypesByCode.get(code)))
//                    .leLabel(buildLeLabel(leTypesByCode.get(code)))
//
//                    .chiTietTrung(buildChiTietJsonForRegion(ketQuaTrongNgay, code, sets))
//                    .build();
//
//            rows.add(kq);
//        }
//
//        return ketQuaTichRepo.saveAll(rows);
//    }
//
//    // ==================== Helper Methods ======================
//
//    private String resolvePlayerName(Long playerId, String playerName,
//                                     TongHopHoaHongLonNhoDto hhln, List<Bet> soList) {
//        String name = playerName;
//        if (isBlank(name) && hhln != null && !isBlank(hhln.getPlayerName())) {
//            name = hhln.getPlayerName();
//        }
//        if (isBlank(name)) {
//            name = playerRepository.findById(playerId).map(Player::getName).orElse(null);
//        }
//        if (isBlank(name) && !soList.isEmpty() && soList.get(0).getPlayer() != null) {
//            name = soList.get(0).getPlayer().getName();
//        }
//        return name;
//    }
//
//    // Build JSON chi tiết cho từng miền – CHỈ kèo THƯỜNG, bỏ L/N & CHẴN/LẺ
//    private String buildChiTietJsonForRegion(List<KetQuaNguoiChoi> all,
//                                             String code,
//                                             Map<String, Set<String>> sets) {
//        try {
//            if (all == null || all.isEmpty()) return "[]";
//
//            List<WinDetail> list = new ArrayList<>();
//            for (KetQuaNguoiChoi k : all) {
//                String codeOfRow = toCode(k.getMien(), sets);
//                if (!code.equals(codeOfRow)) continue;
//
//                if (Boolean.FALSE.equals(k.getTrung())) continue;
//
//                String cachNorm = normalizeNoAccent(k.getCachDanh());
//
//                if (cachNorm.contains("LON") || cachNorm.contains("NHO")) continue;
//
//                // ✅ chỉ loại CHẴN/LẺ thật; KHÔNG loại 2/3 CHÂN nữa (do isChanKeoNorm đã FIX)
//                if (isChanKeoNorm(cachNorm) || isLeKeoNorm(cachNorm)) continue;
//
//                Double tien = k.getTienTrung() != null ? k.getTienTrung() : 0d;
//
//                list.add(new WinDetail(
//                        k.getTenDai(),
//                        k.getCachDanh(),
//                        k.getSoDanh(),
//                        k.getGiaiTrung(),
//                        tien
//                ));
//            }
//
//            if (list.isEmpty()) return "[]";
//            return objectMapper.writeValueAsString(list);
//        } catch (Exception e) {
//            log.error("Lỗi buildChiTietJsonForRegion", e);
//            return "[]";
//        }
//    }
//
//    // --- Helper build label LỚN / NHỎ ---
//
//    private static String buildLonLabel(Set<String> labels) {
//        if (labels == null || labels.isEmpty()) return null;
//
//        if (labels.contains("LỚN ĐẦU") && labels.contains("LỚN ĐUÔI")) return "LỚN ĐẦU/ĐUÔI";
//        if (labels.contains("LỚN ĐẦU")) return "LỚN ĐẦU";
//        if (labels.contains("LỚN ĐUÔI")) return "LỚN ĐUÔI";
//        if (labels.contains("LỚN")) return "LỚN";
//        return labels.iterator().next();
//    }
//
//    private static String buildNhoLabel(Set<String> labels) {
//        if (labels == null || labels.isEmpty()) return null;
//
//        if (labels.contains("NHỎ ĐẦU") && labels.contains("NHỎ ĐUÔI")) return "NHỎ ĐẦU/ĐUÔI";
//        if (labels.contains("NHỎ ĐẦU")) return "NHỎ ĐẦU";
//        if (labels.contains("NHỎ ĐUÔI")) return "NHỎ ĐUÔI";
//        if (labels.contains("NHỎ")) return "NHỎ";
//        return labels.iterator().next();
//    }
//
//    // --- Helper build label CHẴN / LẺ ---
//
//    private static String buildChanLabel(Set<String> labels) {
//        if (labels == null || labels.isEmpty()) return null;
//
//        if (labels.contains("CHẴN ĐẦU") && labels.contains("CHẴN ĐUÔI")) return "CHẴN ĐẦU/ĐUÔI";
//        if (labels.contains("CHẴN ĐẦU")) return "CHẴN ĐẦU";
//        if (labels.contains("CHẴN ĐUÔI")) return "CHẴN ĐUÔI";
//        if (labels.contains("CHẴN")) return "CHẴN";
//        return labels.iterator().next();
//    }
//
//    private static String buildLeLabel(Set<String> labels) {
//        if (labels == null || labels.isEmpty()) return null;
//
//        if (labels.contains("LẺ ĐẦU") && labels.contains("LẺ ĐUÔI")) return "LẺ ĐẦU/ĐUÔI";
//        if (labels.contains("LẺ ĐẦU")) return "LẺ ĐẦU";
//        if (labels.contains("LẺ ĐUÔI")) return "LẺ ĐUÔI";
//        if (labels.contains("LẺ")) return "LẺ";
//        return labels.iterator().next();
//    }
//
//    // parse được cả 3 CHÂN kiểu "10000-20000-30000"
//    private static BigDecimal parseTienDanh(String s) {
//        if (s == null || s.isBlank()) return BigDecimal.ZERO;
//
//        String cleaned = s.replace(".", "").replace(",", "").trim();
//
//        if (cleaned.contains("-")) {
//            BigDecimal sum = BigDecimal.ZERO;
//            String[] parts = cleaned.split("-");
//            for (String part : parts) {
//                String p = part.trim();
//                if (p.isEmpty()) continue;
//                try {
//                    sum = sum.add(new BigDecimal(p));
//                } catch (NumberFormatException ignored) {}
//            }
//            return sum;
//        }
//
//        try {
//            return new BigDecimal(cleaned);
//        } catch (Exception e) {
//            return BigDecimal.ZERO;
//        }
//    }
//
//    private static boolean isBlank(String x) {
//        return x == null || x.trim().isEmpty();
//    }
//    private int tinhSoDai(KetQuaNguoiChoi k) {
//        String dai = k.getTenDai(); // hoặc getTenDai()
//
//        if (dai == null || dai.isBlank()) return 1;
//
//        String n = Normalizer.normalize(dai, Normalizer.Form.NFD)
//                .replaceAll("\\p{M}+", "")
//                .toUpperCase();
//
//        if (n.contains("3 DAI")) return 3;
//        if (n.contains("2 DAI")) return 2;
//
//        if (dai.contains(",")) {
//            return (int) Arrays.stream(dai.split(","))
//                    .map(String::trim)
//                    .filter(s -> !s.isEmpty())
//                    .count();
//        }
//
//        return 1;
//    }
//
//    private static BigDecimal bd(Object x) {
//        if (x == null) return BigDecimal.ZERO;
//        if (x instanceof BigDecimal b) return b;
//        if (x instanceof Double d) return BigDecimal.valueOf(d);
//        return new BigDecimal(x.toString());
//    }
//}
package com.example.doxoso.service;

import com.example.doxoso.model.*;
import com.example.doxoso.repository.BetRepository;
import com.example.doxoso.repository.KetQuaNguoiChoiRepository;
import com.example.doxoso.repository.KetQuaTichRepository;
import com.example.doxoso.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KetQuaTichService {

    private final KetQuaTichRepository ketQuaTichRepo;
    private final BetRepository betRepository;
    private final PlayerRepository playerRepository;
    private final KetQuaNguoiChoiRepository ketQuaNguoiChoiRepo;

    private final TongTienTrungService tongTienTrungService;
    private final TongHopHoaHongLonNhoService tongHopHoaHongLonNhoService;
    private final TongTienAnThuaMienService tongTienAnThuaMienService;
    private final LichQuayXoSoService lichQuayXoSoService;
    private final KetQuaService ketQuaService;
    private final TongTienDanhTheoMienService tongTienDanhTheoMienService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== normalize / alias =====
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("TP.HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("TP HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("TP HCM", "TP HO CHI MINH"),
            Map.entry("HCM", "TP HO CHI MINH"),
            Map.entry("HO CHI MINH", "TP HO CHI MINH"),
            Map.entry("HO CHI MINH CITY", "TP HO CHI MINH"),
            Map.entry("BA RIA VUNG TAU", "VUNG TAU"),
            Map.entry("BRVT", "VUNG TAU"),
            Map.entry("TAYNINH", "TAY NINH"),
            Map.entry("BINHDUONG", "BINH DUONG"),
            Map.entry("BINHPHUOC", "BINH PHUOC"),
            Map.entry("DA LAT", "DA LAT"),
            Map.entry("CAN THO", "CAN THO"),
            Map.entry("TP.CAN THO", "CAN THO")
    );

    /**
     * Chuẩn hóa chuỗi (Fix lỗi đ -> d) để nhận diện đài chính xác
     */
    private static String normalizeNoAccent(String s) {
        if (s == null) return "";
        // 1. Thay thế thủ công đ -> d
        String temp = s.replace("đ", "d").replace("Đ", "D");
        // 2. Normalize
        String noAccent = Normalizer.normalize(temp, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return noAccent
                .toUpperCase()
                .trim()
                .replaceAll("[\\-_.]+", " ")
                .replaceAll("\\s+", " ");
    }

    private static String canonicalProvince(String raw) {
        String u = normalizeNoAccent(raw);
        String canon = ALIASES.getOrDefault(u, u);
        canon = canon.replaceFirst("^TP\\s+", "").trim();
        return canon;
    }

    private Map<String, Set<String>> scheduleSets(LocalDate ngay) {
        Map<String, Set<String>> out = new HashMap<>();
        out.put("MB", new HashSet<>());
        out.put("MT", new HashSet<>());
        out.put("MN", new HashSet<>());

        LichQuayXoSo lich = lichQuayXoSoService.traCuuTheoNgay(ngay);
        Map<String, List<String>> m = lich.getKetQua();

        for (String key : List.of("MIỀN BẮC", "MIỀN TRUNG", "MIỀN NAM")) {
            List<String> list = m.getOrDefault(key, List.of());
            Set<String> target = switch (key) {
                case "MIỀN BẮC" -> out.get("MB");
                case "MIỀN TRUNG" -> out.get("MT");
                case "MIỀN NAM" -> out.get("MN");
                default -> new HashSet<>();
            };
            for (String province : list) {
                target.add(canonicalProvince(province));
            }
        }
        return out;
    }

    private String toCode(String raw, Map<String, Set<String>> sets) {
        String u = normalizeNoAccent(raw);
        if (u.isEmpty()) return "";
        if (u.startsWith("MB") || u.contains("MIEN BAC")) return "MB";
        if (u.startsWith("MT") || u.contains("MIEN TRUNG")) return "MT";
        if (u.startsWith("MN") || u.contains("MIEN NAM")) return "MN";

        String token = canonicalProvince(u);
        for (String key : List.of("MB", "MT", "MN")) {
            if (sets.get(key).contains(token)) return key;
        }
        return u;
    }

    private static String display(String code) {
        return switch (code) {
            case "MB" -> "MIỀN BẮC";
            case "MT" -> "MIỀN TRUNG";
            case "MN" -> "MIỀN NAM";
            default -> code;
        };
    }

    public static class WinDetail {
        public String dai;
        public String cachDanh;
        public String soDanh;
        public String giai;
        public Double tienTrung;

        public WinDetail(String dai, String cachDanh, String soDanh, String giai, Double tienTrung) {
            this.dai = dai;
            this.cachDanh = cachDanh;
            this.soDanh = soDanh;
            this.giai = giai;
            this.tienTrung = tienTrung;
        }
    }

    private static boolean isHaiBaChanNorm(String cachNorm) {
        return cachNorm != null && cachNorm.matches("^(2|3)\\s*CHAN(\\s|$).*");
    }

    private static boolean isChanKeoNorm(String cachNorm) {
        if (cachNorm == null || cachNorm.isBlank()) return false;
        return cachNorm.matches("^CHAN(\\s|$).*") && !isHaiBaChanNorm(cachNorm);
    }

    private static boolean isLeKeoNorm(String cachNorm) {
        if (cachNorm == null || cachNorm.isBlank()) return false;
        return cachNorm.matches("^LE(\\s|$).*");
    }

    // =======================================================================
    //  CHÍNH: chạy & lưu kết quả tịch cho 1 người chơi
    // =======================================================================
    @Transactional
    public List<KetQuaTich> runAndSaveForPlayer(Long playerId, String playerName, LocalDate ngay) {

        // 1️⃣ LẤY BET MỚI NHẤT
        List<Bet> soList = betRepository.findByPlayer_IdAndNgay(playerId, ngay);

        // 2️⃣ XOÁ TOÀN BỘ SNAPSHOT & CHI TIẾT CŨ
        ketQuaTichRepo.deleteByPlayerIdAndNgay(playerId, ngay);
        ketQuaNguoiChoiRepo.deleteByPlayerIdAndNgayChoi(playerId, ngay);

        if (soList.isEmpty()) {
            return List.of();
        }

        // 3️⃣ TÍNH LẠI KẾT QUẢ
        ketQuaService.doKetQua(soList);

        Map<String, Set<String>> sets = scheduleSets(ngay);

        TongHopHoaHongLonNhoDto hhln =
                tongHopHoaHongLonNhoService.tongHopMotNgay(playerId, playerName, ngay);

        // 4️⃣ TIỀN ĐÁNH THEO BET (Lớn/Nhỏ/Chẵn/Lẻ)
        Map<String, BigDecimal> tienLonByCode = new HashMap<>();
        Map<String, BigDecimal> tienNhoByCode = new HashMap<>();
        Map<String, BigDecimal> tienChanByCode = new HashMap<>();
        Map<String, BigDecimal> tienLeByCode = new HashMap<>();

        Map<String, Set<String>> lonTypesByCode = new HashMap<>();
        Map<String, Set<String>> nhoTypesByCode = new HashMap<>();
        Map<String, Set<String>> chanTypesByCode = new HashMap<>();
        Map<String, Set<String>> leTypesByCode = new HashMap<>();

        for (Bet so : soList) {
            BigDecimal stake = parseTienDanh(so.getSoTien());

            // ⚠️ [FIX 1] QUAN TRỌNG: Phải nhân số đài ở đây
            long soDai = tinhSoDaiTuBet(so);
            if (soDai > 1) {
                stake = stake.multiply(BigDecimal.valueOf(soDai));
            }

            String code = toCode(so.getMien(), sets);
            if (!Set.of("MB", "MT", "MN").contains(code)) continue;

            String cach = normalizeNoAccent(so.getCachDanh());

            if (cach.contains("LON")) {
                tienLonByCode.merge(code, stake, BigDecimal::add);
                lonTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
                        .add(cach.contains("DAU") ? "LỚN ĐẦU" :
                                cach.contains("DUOI") ? "LỚN ĐUÔI" : "LỚN");
            }

            if (cach.contains("NHO")) {
                tienNhoByCode.merge(code, stake, BigDecimal::add);
                nhoTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
                        .add(cach.contains("DAU") ? "NHỎ ĐẦU" :
                                cach.contains("DUOI") ? "NHỎ ĐUÔI" : "NHỎ");
            }

            if (isChanKeoNorm(cach)) {
                tienChanByCode.merge(code, stake, BigDecimal::add);
                chanTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
                        .add(cach.contains("DAU") ? "CHẴN ĐẦU" :
                                cach.contains("DUOI") ? "CHẴN ĐUÔI" : "CHẴN");
            }

            if (isLeKeoNorm(cach)) {
                tienLeByCode.merge(code, stake, BigDecimal::add);
                leTypesByCode.computeIfAbsent(code, k -> new HashSet<>())
                        .add(cach.contains("DAU") ? "LẺ ĐẦU" :
                                cach.contains("DUOI") ? "LẺ ĐUÔI" : "LẺ");
            }
        }

        // 5️⃣ TIỀN ĐÁNH THƯỜNG (Tổng hợp từ service kia)
        Map<String, BigDecimal> tienDanhByCode = new HashMap<>();
        tongTienDanhTheoMienService
                .tinhTongTheoMienTheoNgay(playerId, ngay, ngay)
                .stream().findFirst().ifPresent(dto -> {
            tienDanhByCode.put("MB", bd(dto.getMienBac()));
            tienDanhByCode.put("MT", bd(dto.getMienTrung()));
            tienDanhByCode.put("MN", bd(dto.getMienNam()));
        });

        Double hoaHongPlayer = playerRepository.findById(playerId)
                .map(Player::getHoaHong).orElse(null);

        List<KetQuaNguoiChoi> ketQuaTrongNgay =
                ketQuaNguoiChoiRepo.findByPlayerIdAndNgayChoi(playerId, ngay);

        Map<String, BigDecimal> tienTrungThuongByCode = new HashMap<>();
        Map<String, BigDecimal> tienLonNetByCode = new HashMap<>();
        Map<String, BigDecimal> tienNhoNetByCode = new HashMap<>();
        Map<String, BigDecimal> tienChanNetByCode = new HashMap<>();
        Map<String, BigDecimal> tienLeNetByCode = new HashMap<>();

        for (KetQuaNguoiChoi k : ketQuaTrongNgay) {
            String code = toCode(k.getMien(), sets);
            if (!Set.of("MB", "MT", "MN").contains(code)) continue;

            String cach = normalizeNoAccent(k.getCachDanh());

            boolean lon = cach.contains("LON");
            boolean nho = cach.contains("NHO");
            boolean chan = isChanKeoNorm(cach);
            boolean le = isLeKeoNorm(cach);

            BigDecimal tienTrung = bd(k.getTienTrung());

            // KÈO THƯỜNG
            if (!lon && !nho && !chan && !le) {
                if (Boolean.TRUE.equals(k.getTrung())) {
                    tienTrungThuongByCode.merge(code, tienTrung, BigDecimal::add);
                }
                continue;
            }

            // KÈO ĐẶC BIỆT (Lớn/Nhỏ/Chẵn/Lẻ) - Tính Net
            // [FIX 1] Đã nhân đài ở đây rồi, nhưng cần đảm bảo tinhSoDai chính xác
            BigDecimal stakeMotDai = parseTienDanh(k.getTienDanh() + "");
            int soDai = tinhSoDaiTuKetQua(k); // Dùng hàm mới chuẩn hơn

            BigDecimal stakeTong = stakeMotDai.multiply(BigDecimal.valueOf(soDai));

            BigDecimal delta;
            if (Boolean.TRUE.equals(k.getTrung())) {
                delta = tienTrung;          // tiền trúng đã là tổng nhận
            } else {
                delta = stakeTong.negate(); // tiền thua = tổng vốn đã cược
            }

            if (lon) tienLonNetByCode.merge(code, delta, BigDecimal::add);
            if (nho) tienNhoNetByCode.merge(code, delta, BigDecimal::add);
            if (chan) tienChanNetByCode.merge(code, delta, BigDecimal::add);
            if (le) tienLeNetByCode.merge(code, delta, BigDecimal::add);
        }

        // 6️⃣ BUILD SNAPSHOT & INSERT
        List<KetQuaTich> rows = new ArrayList<>();

        for (String code : List.of("MB", "MT", "MN")) {
            BigDecimal tienDanh = tienDanhByCode.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal tienTrung = tienTrungThuongByCode.getOrDefault(code, BigDecimal.ZERO);

            BigDecimal tienHH = hoaHongPlayer == null ? BigDecimal.ZERO :
                    tienDanh.multiply(bd(hoaHongPlayer).divide(BigDecimal.valueOf(100)))
                            .setScale(2, BigDecimal.ROUND_HALF_UP);

            // ⚠️ [FIX 2] QUAN TRỌNG: CÔNG THỨC ĂN THUA ĐÚNG
            // Ăn Thua = (Tiền Trúng + Tiền Hoa Hồng) - Tiền Đánh
            // Ví dụ: Đánh 100, HH 10, Trúng 0 => (0 + 10) - 100 = -90 (Thua 90)
            // ✅ CÔNG THỨC MỚI (Đồng bộ với yêu cầu của bạn):
// Ăn Thua = Trúng - Hoa Hồng
            BigDecimal tienAT = tienTrung.subtract(tienHH).setScale(2, BigDecimal.ROUND_HALF_UP);

            KetQuaTich kq = KetQuaTich.builder()
                    .playerId(playerId)
                    .playerName(resolvePlayerName(playerId, playerName, hhln, soList))
                    .ngay(ngay)
                    .mienCode(code)
                    .mienDisplay(display(code))

                    .tienTrung(tienTrung)
                    .tienHoaHong(tienHH)

                    // Tổng Lớn Nhỏ Net
                    .tienLonNho(
                            tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO)
                                    .add(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO))
                                    .setScale(2, BigDecimal.ROUND_HALF_UP)
                    )

                    // Tiền Ăn Thua (Đã sửa công thức)
                    .tienAnThua(tienAT)

                    .tienDanh(tienDanh)
                    .tienDanhDaNhanHoaHong(tienHH)
                    .tienDanhDaNhanHoaHongCongLonNho(
                            tienHH.add(
                                    tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO)
                                            .add(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO))
                            )
                    )

                    // LỚN / NHỎ
                    .tienLonDanh(tienLonByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienNhoDanh(tienNhoByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienLonNet(tienLonNetByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienNhoNet(tienNhoNetByCode.getOrDefault(code, BigDecimal.ZERO))
                    .lonLabel(buildLonLabel(lonTypesByCode.get(code)))
                    .nhoLabel(buildNhoLabel(nhoTypesByCode.get(code)))

                    // CHẴN / LẺ
                    .tienChanDanh(tienChanByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienLeDanh(tienLeByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienChanNet(tienChanNetByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienLeNet(tienLeNetByCode.getOrDefault(code, BigDecimal.ZERO))
                    .tienChanLe(
                            tienChanNetByCode.getOrDefault(code, BigDecimal.ZERO)
                                    .add(tienLeNetByCode.getOrDefault(code, BigDecimal.ZERO))
                    )
                    .chanLabel(buildChanLabel(chanTypesByCode.get(code)))
                    .leLabel(buildLeLabel(leTypesByCode.get(code)))

                    .chiTietTrung(buildChiTietJsonForRegion(ketQuaTrongNgay, code, sets))
                    .build();

            rows.add(kq);
        }

        return ketQuaTichRepo.saveAll(rows);
    }

    // ==================== Helper Methods (Đã Fix) ======================

    /**
     * [FIX 3] Hàm đếm đài chuẩn (dùng cho Bet)
     */
    private static long tinhSoDaiTuBet(Bet so) {
        String raw = null;
        try {
            // Ưu tiên lấy getDai()
            Object v = so.getClass().getMethod("getDai").invoke(so);
            if (v != null) raw = v.toString();
        } catch (Exception ignored) {}

        if (raw == null) {
            // Fallback lấy getTenDai()
            try {
                Object v = so.getClass().getMethod("getTenDai").invoke(so);
                if (v != null) raw = v.toString();
            } catch (Exception ignored) {}
        }

        return demSoDaiTuChuoi(raw);
    }

    /**
     * [FIX 3] Hàm đếm đài chuẩn (dùng cho KetQuaNguoiChoi)
     */
    private int tinhSoDaiTuKetQua(KetQuaNguoiChoi k) {
        String raw = k.getTenDai();
        // Nếu entity KetQuaNguoiChoi có getDai() thì thêm reflection ở đây
        return (int) demSoDaiTuChuoi(raw);
    }

    /**
     * Logic đếm chung
     */
    private static long demSoDaiTuChuoi(String raw) {
        if (raw == null || raw.isBlank()) return 1L;

        String n = normalizeNoAccent(raw); // Đã xử lý đ -> d

        if (n.contains("4 DAI")) return 4L;
        if (n.contains("3 DAI")) return 3L;
        if (n.contains("2 DAI")) return 2L;

        if (raw.contains(",")) {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .count();
        }
        return 1L;
    }

    private String resolvePlayerName(Long playerId, String playerName,
                                     TongHopHoaHongLonNhoDto hhln, List<Bet> soList) {
        String name = playerName;
        if (isBlank(name) && hhln != null && !isBlank(hhln.getPlayerName())) {
            name = hhln.getPlayerName();
        }
        if (isBlank(name)) {
            name = playerRepository.findById(playerId).map(Player::getName).orElse(null);
        }
        if (isBlank(name) && !soList.isEmpty() && soList.get(0).getPlayer() != null) {
            name = soList.get(0).getPlayer().getName();
        }
        return name;
    }

    private String buildChiTietJsonForRegion(List<KetQuaNguoiChoi> all,
                                             String code,
                                             Map<String, Set<String>> sets) {
        try {
            if (all == null || all.isEmpty()) return "[]";

            List<WinDetail> list = new ArrayList<>();
            for (KetQuaNguoiChoi k : all) {
                String codeOfRow = toCode(k.getMien(), sets);
                if (!code.equals(codeOfRow)) continue;

                if (Boolean.FALSE.equals(k.getTrung())) continue;

                String cachNorm = normalizeNoAccent(k.getCachDanh());

                if (cachNorm.contains("LON") || cachNorm.contains("NHO")) continue;
                if (isChanKeoNorm(cachNorm) || isLeKeoNorm(cachNorm)) continue;

                Double tien = k.getTienTrung() != null ? k.getTienTrung() : 0d;

                list.add(new WinDetail(
                        k.getTenDai(),
                        k.getCachDanh(),
                        k.getSoDanh(),
                        k.getGiaiTrung(),
                        tien
                ));
            }

            if (list.isEmpty()) return "[]";
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Lỗi buildChiTietJsonForRegion", e);
            return "[]";
        }
    }

    // --- Label Helpers ---
    private static String buildLonLabel(Set<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        if (labels.contains("LỚN ĐẦU") && labels.contains("LỚN ĐUÔI")) return "LỚN ĐẦU/ĐUÔI";
        if (labels.contains("LỚN ĐẦU")) return "LỚN ĐẦU";
        if (labels.contains("LỚN ĐUÔI")) return "LỚN ĐUÔI";
        if (labels.contains("LỚN")) return "LỚN";
        return labels.iterator().next();
    }

    private static String buildNhoLabel(Set<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        if (labels.contains("NHỎ ĐẦU") && labels.contains("NHỎ ĐUÔI")) return "NHỎ ĐẦU/ĐUÔI";
        if (labels.contains("NHỎ ĐẦU")) return "NHỎ ĐẦU";
        if (labels.contains("NHỎ ĐUÔI")) return "NHỎ ĐUÔI";
        if (labels.contains("NHỎ")) return "NHỎ";
        return labels.iterator().next();
    }

    private static String buildChanLabel(Set<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        if (labels.contains("CHẴN ĐẦU") && labels.contains("CHẴN ĐUÔI")) return "CHẴN ĐẦU/ĐUÔI";
        if (labels.contains("CHẴN ĐẦU")) return "CHẴN ĐẦU";
        if (labels.contains("CHẴN ĐUÔI")) return "CHẴN ĐUÔI";
        if (labels.contains("CHẴN")) return "CHẴN";
        return labels.iterator().next();
    }

    private static String buildLeLabel(Set<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        if (labels.contains("LẺ ĐẦU") && labels.contains("LẺ ĐUÔI")) return "LẺ ĐẦU/ĐUÔI";
        if (labels.contains("LẺ ĐẦU")) return "LẺ ĐẦU";
        if (labels.contains("LẺ ĐUÔI")) return "LẺ ĐUÔI";
        if (labels.contains("LẺ")) return "LẺ";
        return labels.iterator().next();
    }

    private static BigDecimal parseTienDanh(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        String cleaned = s.replace(".", "").replace(",", "").trim();
        if (cleaned.contains("-")) {
            BigDecimal sum = BigDecimal.ZERO;
            String[] parts = cleaned.split("-");
            for (String part : parts) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                try {
                    sum = sum.add(new BigDecimal(p));
                } catch (NumberFormatException ignored) {}
            }
            return sum;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean isBlank(String x) {
        return x == null || x.trim().isEmpty();
    }

    private static BigDecimal bd(Object x) {
        if (x == null) return BigDecimal.ZERO;
        if (x instanceof BigDecimal b) return b;
        if (x instanceof Double d) return BigDecimal.valueOf(d);
        return new BigDecimal(x.toString());
    }
}