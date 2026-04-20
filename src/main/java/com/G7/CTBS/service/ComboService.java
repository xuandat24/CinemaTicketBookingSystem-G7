package com.G7.CTBS.service;

import com.G7.CTBS.dto.AdminComboDTO;
import com.G7.CTBS.entity.Combo;
import com.G7.CTBS.repository.ComboRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComboService {

    @Autowired
    private ComboRepository comboRepository;

    public List<Combo> getAllCombos() {
        return comboRepository.findAll();
    }

    public List<AdminComboDTO> findAllForAdmin(String keyword) {
        List<Combo> combos;
        if (keyword == null || keyword.trim().isEmpty()) {
            combos = comboRepository.findAll();
        } else {
            combos = comboRepository.findByNameContainingIgnoreCase(keyword.trim());
        }
        return combos.stream().map(this::toDTO).toList();
    }

    public AdminComboDTO findById(Long id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo with id " + id + " not found"));
        return toDTO(combo);
    }

    public void createCombo(AdminComboDTO dto) {
        if (comboRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new EntityExistsException("Combo with name '" + dto.getName() + "' already exists");
        }

        Combo combo = new Combo();
        combo.setName(dto.getName().trim());
        combo.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        combo.setPrice(dto.getPrice() == null ? 0 : dto.getPrice());
        combo.setImage(dto.getImage() == null ? "" : dto.getImage().trim());

        comboRepository.save(combo);
    }

    public void updateCombo(Long id, AdminComboDTO dto) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo with id " + id + " not found"));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            String nextName = dto.getName().trim();
            if (!nextName.equalsIgnoreCase(combo.getName()) && comboRepository.existsByNameIgnoreCase(nextName)) {
                throw new EntityExistsException("Combo with name '" + nextName + "' already exists");
            }
            combo.setName(nextName);
        }

        combo.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        combo.setPrice(dto.getPrice() == null ? 0 : dto.getPrice());
        combo.setImage(dto.getImage() == null ? "" : dto.getImage().trim());

        comboRepository.save(combo);
    }

    public void deleteCombo(Long id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo with id " + id + " not found"));

        try {
            comboRepository.delete(combo);
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete combo because it may already be linked to bookings.");
        }
    }

    private AdminComboDTO toDTO(Combo combo) {
        return AdminComboDTO.builder()
                .id(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .price(combo.getPrice())
                .image(combo.getImage())
                .build();
    }
}