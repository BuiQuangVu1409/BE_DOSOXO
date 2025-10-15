//    package com.example.doxoso.model;
//
//    import com.fasterxml.jackson.annotation.JsonFormat;
//    import com.fasterxml.jackson.annotation.JsonIgnore;
//    import jakarta.persistence.*;
//    import lombok.AllArgsConstructor;
//    import lombok.Getter;
//    import lombok.NoArgsConstructor;
//    import lombok.Setter;
//    import java.time.LocalDate;
//
//
//
//    @AllArgsConstructor
//    @NoArgsConstructor
//    @Getter
//    @Setter
//    @Entity
//    @Table(name = "so_nguoi_choi")
//    public class SoNguoiChoi {
//        @Id
//        @GeneratedValue(strategy = GenerationType.IDENTITY)
//        private Long id;
//
//
//        private String soDanh;
//        private String tenDai;
//        private String mien;
//
//        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
//        private LocalDate ngay;
//
//        private String cachDanh;
//        private String tienDanh;
//        private String ketQua;
//
//        // Quan hệ với Player
//        @ManyToOne
//        @JoinColumn(name = "player_id", referencedColumnName = "id", nullable = false)
//        @JsonIgnore
//        private Player player;
//
//
//    }
