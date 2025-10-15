package com.example.doxoso.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "bets")
@EqualsAndHashCode(of = "id")
@ToString(exclude = "player")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"}) // ẩn proxy Hibernate khi serialize
public class Bet {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String mien;
        private String dai;
        private String cachDanh;
        private String soDanh;
        private String soTien;

        @Column(nullable = false)
        private LocalDate ngay = LocalDate.now();

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "player_id", nullable = false)
        @JsonBackReference                           // đầu “con” của quan hệ -> chặn serialize ngược Player
        private Player player;
}
