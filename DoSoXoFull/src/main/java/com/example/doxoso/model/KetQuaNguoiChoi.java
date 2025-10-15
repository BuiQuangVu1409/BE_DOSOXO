// liên kết với kết quả chi tiết để lưu dữ liệu mỗi lần chạy sẽ lưu vào database
package com.example.doxoso.model;


import jakarta.persistence.*;
import lombok.*;

import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "ket_qua_nguoi_choi")
public class KetQuaNguoiChoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin người chơi
    private Long playerId;
    private String playerName;
    private Double hoaHong;
    private Double heSoCachDanh;

    // Cách đánh, số đánh, miền, đài
    private String cachDanh;
    private String soDanh;
    private String mien;
    private String tenDai;
    private LocalDate ngayChoi;

    // Kết quả trúng/trật
    private Boolean trung;
    private String giaiTrung;   // VD: "Giải Nhất, Giải Đặc Biệt"
    private String soTrung;     // VD: "12345, 67890"
    private String lyDo;        // nếu trật thì lý do

    // Tiền cược và tiền trúng
    private Double tienDanh;
    private Double tienTrung;
    private Double tienTrungBaoLo;
    private Double tienTrungThuong;
    private Double tienTrungDacBiet;

    // Thời điểm dò
    private LocalDateTime thoiGianDo = LocalDateTime.now();


    // NEW: khóa tham chiếu về bản ghi nguồn SoNguoiChoi
    @Column(name = "source_so_id", nullable = false)
    private Long sourceSoId;

    // NEW: đánh dấu bản summary (tổng quát) hay chi tiết
    @Column(name = "is_summary", nullable = false)
    private boolean summary;

    @Version
    private Long version;

}
