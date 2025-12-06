package com.example.doxoso.service;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.Player;
import com.example.doxoso.repository.BetRepository;
import com.example.doxoso.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional // mặc định; các hàm đọc dùng readOnly=true
public class BetService {

    private final BetRepository betRepository;
    private final PlayerRepository playerRepository;

    // [MỚI] thêm dependency để sau khi Bet đổi thì chạy lại kết quả
    private final KetQuaTichService ketQuaTichService;

    // [SỬA NHẸ] thêm ketQuaTichService vào constructor
    public BetService(BetRepository betRepository,
                      PlayerRepository playerRepository,
                      KetQuaTichService ketQuaTichService) {
        this.betRepository = betRepository;
        this.playerRepository = playerRepository;
        this.ketQuaTichService = ketQuaTichService;
    }

    /* ===================== READ ===================== */

    @Transactional(readOnly = true)
    public Bet getById(Long id) {
        return mustGet(id);
    }

    @Transactional(readOnly = true)
    public List<Bet> getByPlayerId(Long playerId) {
        // KHÔNG ném exception khi rỗng -> tránh 500 cho case hợp lệ "chưa có bet"
        return betRepository.findByPlayer_Id(playerId);
    }

    @Transactional(readOnly = true)
    public List<Bet> listAll() {
        return betRepository.findAll(); // repo đã @EntityGraph(fetch player)
    }

    @Transactional(readOnly = true)
    public List<Bet> search(Long playerId, LocalDate from, LocalDate to, String mien, String dai) {
        if (playerId != null && from != null && to != null)
            return betRepository.findByPlayer_IdAndNgayBetween(playerId, from, to);
        if (from != null && to != null)
            return betRepository.findByNgayBetween(from, to);
        if (playerId != null && mien != null && dai != null)
            return betRepository.findByPlayer_IdAndMienContainingIgnoreCaseAndDaiContainingIgnoreCase(playerId, mien, dai);
        if (playerId != null)
            return getByPlayerId(playerId);
        return listAll();
    }

    /* ================= CREATE / REPLACE / PATCH / DELETE ================= */

    public Bet create(Bet req) {
        Bet bet = new Bet();
        bet.setPlayer(resolvePlayerForUpsert(req.getPlayer()));
        bet.setMien(req.getMien());
        bet.setDai(req.getDai());
        bet.setCachDanh(req.getCachDanh());
        bet.setSoDanh(req.getSoDanh());
        bet.setSoTien(req.getSoTien());
        bet.setNgay(req.getNgay() != null ? req.getNgay() : LocalDate.now());
        Bet saved = betRepository.save(bet);

        // [MỚI - TÙY CHỌN] nếu bạn muốn khi tạo bet mới cũng chạy lại kết quả
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet replace(Long id, Bet req) {
        Bet bet = mustGet(id);
        bet.setPlayer(resolvePlayerForUpsert(req.getPlayer()));
        bet.setMien(req.getMien());
        bet.setDai(req.getDai());
        bet.setCachDanh(req.getCachDanh());
        bet.setSoDanh(req.getSoDanh());
        bet.setSoTien(req.getSoTien());
        bet.setNgay(req.getNgay() != null ? req.getNgay()
                : (bet.getNgay() == null ? LocalDate.now() : bet.getNgay()));
        Bet saved = betRepository.save(bet);

        // [MỚI] Bet này đã đổi hoàn toàn -> chạy lại KQ cho player + ngày của bet
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet patch(Long id, Bet req) {
        Bet bet = mustGet(id);
        if (req.getMien() != null)      bet.setMien(req.getMien());
        if (req.getDai() != null)       bet.setDai(req.getDai());
        if (req.getCachDanh() != null)  bet.setCachDanh(req.getCachDanh());
        if (req.getSoDanh() != null)    bet.setSoDanh(req.getSoDanh());
        if (req.getSoTien() != null)    bet.setSoTien(req.getSoTien());
        if (req.getNgay() != null)      bet.setNgay(req.getNgay());
        if (req.getPlayer() != null)    bet.setPlayer(resolvePlayerForUpsert(req.getPlayer()));
        Bet saved = betRepository.save(bet);

        // [MỚI] patch xong -> chạy lại KQ cho player + ngày tương ứng
        recalcKetQuaForBet(saved);

        return saved;
    }

    public void delete(Long id) {
        Bet bet = mustGet(id); // [SỬA NHẸ] lấy bet trước khi xóa để biết player + ngày
        Long playerId = bet.getPlayer().getId();
        String playerName = bet.getPlayer().getName();
        LocalDate ngay = bet.getNgay();

        if (!betRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tồn tại Bet id: " + id);

        betRepository.deleteById(id);

        // [MỚI] sau khi xóa bet -> chạy lại KQ cho player + ngày đó
        recalcKetQuaForPlayerNgay(playerId, playerName, ngay);
    }

    /* =============== PATCH RIÊNG LẺ CHO CONTROLLER =============== */

    public Bet updateSoTien(Long id, String soTien) {
        if (soTien == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "soTien required");
        Bet b = mustGet(id);
        b.setSoTien(soTien.trim());
        Bet saved = betRepository.save(b);

        // [MỚI]
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet updateSoDanh(Long id, String soDanh) {
        if (soDanh == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "soDanh required");
        Bet b = mustGet(id);
        b.setSoDanh(soDanh.trim());
        Bet saved = betRepository.save(b);

        // [MỚI]
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet updateCachDanh(Long id, String cachDanh) {
        if (cachDanh == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cachDanh required");
        Bet b = mustGet(id);
        b.setCachDanh(cachDanh.trim());
        Bet saved = betRepository.save(b);

        // [MỚI]
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet updateMien(Long id, String mien) {
        if (mien == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mien required");
        Bet b = mustGet(id);
        b.setMien(mien.trim());
        Bet saved = betRepository.save(b);

        // [MỚI]
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet updateDai(Long id, String dai) {
        if (dai == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dai required");
        Bet b = mustGet(id);
        b.setDai(dai.trim());
        Bet saved = betRepository.save(b);

        // [MỚI]
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet updateNgay(Long id, LocalDate ngay) {
        if (ngay == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ngay required");
        Bet b = mustGet(id);
        LocalDate oldNgay = b.getNgay();
        Long playerId = b.getPlayer().getId();
        String playerName = b.getPlayer().getName();

        b.setNgay(ngay);
        Bet saved = betRepository.save(b);

        // [MỚI] case đặc biệt: đổi ngày -> nên chạy lại KQ cho cả ngày cũ & ngày mới
        recalcKetQuaForPlayerNgay(playerId, playerName, oldNgay);
        recalcKetQuaForBet(saved);

        return saved;
    }

    public Bet changePlayer(Long betId, Long playerId) {
        Bet bet = mustGet(betId);
        Long oldPlayerId = bet.getPlayer().getId();
        String oldPlayerName = bet.getPlayer().getName();
        LocalDate ngay = bet.getNgay();

        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Player id: " + playerId));
        bet.setPlayer(p);
        Bet saved = betRepository.save(bet);

        // [MỚI] đổi player -> chạy lại KQ cho player cũ & player mới trong ngày đó
        recalcKetQuaForPlayerNgay(oldPlayerId, oldPlayerName, ngay);
        recalcKetQuaForBet(saved);

        return saved;
    }

    /* ===================== HELPERS ===================== */

    private Bet mustGet(Long id) {
        return betRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Bet với id: " + id));
    }

    private Player resolvePlayerForUpsert(Player input) {
        if (input == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin player (player hoặc player.id).");

        if (input.getId() != null) {
            return playerRepository.findById(input.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Player id: " + input.getId()));
        }

        if (input.getName() == null || input.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tạo Player mới cần tối thiểu trường name.");

        Player p = new Player();
        p.setName(input.getName());
        p.setHoaHong(input.getHoaHong());
        p.setHeSoCachDanh(input.getHeSoCachDanh());
        return playerRepository.save(p);
    }

    /* ===== giữ các tên cũ để tương thích controller cũ (nếu còn dùng) ===== */

    @Transactional(readOnly = true)
    public Bet getSoNguoiChoiById(Long id) { return getById(id); }

    @Transactional(readOnly = true)
    public List<Bet> getSoNguoiChoiByPlayerId(Long playerId) { return getByPlayerId(playerId); }

    /* ===================== [MỚI] HÀM GỌI LẠI KẾT QUẢ ===================== */

    /**
     * Gọi lại pipeline runAndSaveForPlayer hiện có,
     * để cập nhật KETQUANGUOICHOI + KETQUATICH cho đúng player + ngày của Bet.
     */
    private void recalcKetQuaForBet(Bet bet) {
        if (bet == null || bet.getPlayer() == null || bet.getNgay() == null) return;
        recalcKetQuaForPlayerNgay(
                bet.getPlayer().getId(),
                bet.getPlayer().getName(),
                bet.getNgay()
        );
    }

    private void recalcKetQuaForPlayerNgay(Long playerId, String playerName, LocalDate ngay) {
        try {
            // TẬN DỤNG lại logic cũ: run & save full cho player + ngày đó
            ketQuaTichService.runAndSaveForPlayer(playerId, playerName, ngay);
        } catch (Exception e) {
            // Nếu có lỗi -> rollback toàn bộ transaction (Bet + KQ)
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi cập nhật lại kết quả cho player " + playerId + " ngày " + ngay + ": " + e.getMessage(),
                    e
            );
        }
    }
}
