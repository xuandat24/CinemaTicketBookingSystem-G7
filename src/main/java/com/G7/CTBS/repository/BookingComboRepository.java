package com.G7.CTBS.repository;

import com.G7.CTBS.dto.ComboStatDTO;
import com.G7.CTBS.entity.BookingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingComboRepository extends JpaRepository<BookingCombo, Long> {

    @Query("SELECT SUM(bc.quantity) " +
            "FROM BookingCombo bc JOIN bc.booking b " +
            "WHERE b.status IN ('SUCCESS', 'CONFIRMED') AND b.createTime BETWEEN :start AND :end")
    Long getTotalCombos(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.G7.CTBS.dto.ComboStatDTO(c.name, SUM(bc.quantity)) " +
            "FROM BookingCombo bc JOIN bc.combo c JOIN bc.booking b " +
            "WHERE b.status IN ('SUCCESS', 'CONFIRMED') AND b.createTime BETWEEN :start AND :end " +
            "GROUP BY c.id, c.name " +
            "ORDER BY SUM(bc.quantity) DESC")
    List<ComboStatDTO> getComboStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
