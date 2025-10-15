package com.example.doxoso.service;

import com.example.doxoso.model.DoiChieuKetQuaDto;

import java.util.List;

public interface IKetQuaService {

    /**
     * Đối chiếu toàn bộ số người chơi với kết quả xổ số
     */
    List<DoiChieuKetQuaDto> doiChieuTatCaSo();

    /**
     * Lấy danh sách số đã trúng
     */
    List<DoiChieuKetQuaDto> layDanhSachSoTrung();

    /**
     * Lấy danh sách số đã trật
     */
    List<DoiChieuKetQuaDto> layDanhSachSoTrat();

    /**
     * Lọc theo kết quả (trúng / trật) và loại cách đánh (xuyên, 2 chân, đầu, đuôi, đầu đuôi...)
     * @param trungOrTrat "trung" hoặc "trat"
     * @param cachDanh tên cách đánh (vd: "xuyen", "2chan", "dau", "duoi", "dauduoi", ...)
     * @return danh sách đối chiếu phù hợp
     */
    List<DoiChieuKetQuaDto> locTheoKetQuaVaCachDanh(String trungOrTrat, String cachDanh);
}
