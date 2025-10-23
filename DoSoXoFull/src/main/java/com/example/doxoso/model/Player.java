package com.example.doxoso.model;

import com.example.doxoso.model.Bet;
import com.example.doxoso.model.DoiChieuKetQuaDto;
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
@EqualsAndHashCode(of = "id")
@ToString(exclude = "bets")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Player {

        @Id
        // @GeneratedValue(strategy = GenerationType.IDENTITY) // ❌ bỏ vì ID client cấp
        private Long id;

        @Version
        private Long version; // ✅ thêm để khóa lạc quan

        private String name;
        private String phone;
        private Double hoaHong;
        private Double heSoCachDanh;

        @Transient
        private List<DoiChieuKetQuaDto> ketQua;

        @OneToMany(
                mappedBy = "player",
                cascade = CascadeType.ALL,
                orphanRemoval = true,
                fetch = FetchType.LAZY
        )
        @JsonManagedReference
        private List<Bet> bets = new ArrayList<>();

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
