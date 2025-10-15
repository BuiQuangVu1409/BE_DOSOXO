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
import java.util.Collections;
import java.util.List;

@Service
@Transactional // ghi mặc định; các hàm đọc dùng readOnly=true
public class BetService {

    private final BetRepository betRepository;
    private final PlayerRepository playerRepository;

    public BetService(BetRepository betRepository, PlayerRepository playerRepository) {
        this.betRepository = betRepository;
        this.playerRepository = playerRepository;
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
        return betRepository.save(bet);
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
        return betRepository.save(bet);
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
        return betRepository.save(bet);
    }

    public void delete(Long id) {
        if (!betRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tồn tại Bet id: " + id);
        betRepository.deleteById(id);
    }

    /* =============== PATCH RIÊNG LẺ CHO CONTROLLER =============== */

    public Bet updateSoTien(Long id, String soTien) {
        if (soTien == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "soTien required");
        Bet b = mustGet(id);
        b.setSoTien(soTien.trim());
        return betRepository.save(b);
    }

    public Bet updateSoDanh(Long id, String soDanh) {
        if (soDanh == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "soDanh required");
        Bet b = mustGet(id);
        b.setSoDanh(soDanh.trim());
        return betRepository.save(b);
    }

    public Bet updateCachDanh(Long id, String cachDanh) {
        if (cachDanh == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cachDanh required");
        Bet b = mustGet(id);
        b.setCachDanh(cachDanh.trim());
        return betRepository.save(b);
    }

    public Bet updateMien(Long id, String mien) {
        if (mien == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mien required");
        Bet b = mustGet(id);
        b.setMien(mien.trim());
        return betRepository.save(b);
    }

    public Bet updateDai(Long id, String dai) {
        if (dai == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dai required");
        Bet b = mustGet(id);
        b.setDai(dai.trim());
        return betRepository.save(b);
    }

    public Bet updateNgay(Long id, LocalDate ngay) {
        if (ngay == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ngay required");
        Bet b = mustGet(id);
        b.setNgay(ngay);
        return betRepository.save(b);
    }

    public Bet changePlayer(Long betId, Long playerId) {
        Bet bet = mustGet(betId);
        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Player id: " + playerId));
        bet.setPlayer(p);
        return betRepository.save(bet);
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
}
