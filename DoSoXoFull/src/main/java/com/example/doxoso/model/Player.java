package com.example.doxoso.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "players")
@EqualsAndHashCode(of = "id")          // tránh đệ quy khi so sánh (chỉ dựa vào id)
@ToString(exclude = "bets")            // tránh vòng lặp khi log/print
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"}) // tránh field lazy của Hibernate lọt ra JSON
public class Player {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;
        private String phone;
        private Double hoaHong;
        private Double heSoCachDanh;

        // ✅ chỉ dùng để build DTO, không lưu DB
        @Transient
        private List<DoiChieuKetQuaDto> ketQua;

        @OneToMany(
                mappedBy = "player",
                cascade = CascadeType.ALL,
                orphanRemoval = true,          // xóa Bet mồ côi khi remove khỏi list
                fetch = FetchType.LAZY
        )
        @JsonManagedReference               // đầu quan hệ; Jackson sẽ serialize list bets nhưng KHÔNG serialize ngược lại player trong Bet
        private List<Bet> bets = new ArrayList<>();

        // ===== Conveniences để đồng bộ 2 chiều =====
        public void addBet(Bet bet) {
                if (bet == null) return;
                bets.add(bet);
                bet.setPlayer(this);
        }

        public void removeBet(Bet bet) {
                if (bet == null) return;
                bets.remove(bet);
                bet.setPlayer(null);
        }
}
