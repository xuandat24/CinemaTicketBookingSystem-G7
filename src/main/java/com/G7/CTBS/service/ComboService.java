package com.G7.CTBS.service;

import com.G7.CTBS.entity.Combo;
import com.G7.CTBS.repository.ComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComboService {

    @Autowired
    private ComboRepository comboRepository;

    public List<Combo> getAllCombos(){
        return comboRepository.findAll();
    }
}