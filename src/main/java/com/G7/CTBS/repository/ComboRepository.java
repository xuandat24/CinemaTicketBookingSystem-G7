package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByNameContainingIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
