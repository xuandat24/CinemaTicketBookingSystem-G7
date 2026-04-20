package com.G7.CTBS.controller;

import com.G7.CTBS.dto.AdminComboDTO;
import com.G7.CTBS.service.ComboService;
import com.G7.CTBS.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/combos")
public class ComboController {

    private final ComboService comboService;
    private final FileStorageService fileStorageService;

    public ComboController(ComboService comboService, FileStorageService fileStorageService) {
        this.comboService = comboService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<AdminComboDTO>> getCombos(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(comboService.findAllForAdmin(name));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminComboDTO> getComboById(@PathVariable Long id) {
        return ResponseEntity.ok(comboService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createCombo(@Valid @RequestBody AdminComboDTO dto) {
        comboService.createCombo(dto);
        return ResponseEntity.ok(Map.of("message", "Combo added successfully!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateCombo(@PathVariable Long id, @Valid @RequestBody AdminComboDTO dto) {
        comboService.updateCombo(id, dto);
        return ResponseEntity.ok(Map.of("message", "Combo updated successfully!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCombo(@PathVariable Long id) {
        comboService.deleteCombo(id);
        return ResponseEntity.ok(Map.of("message", "Combo deleted successfully!"));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "name", required = false) String name) {
        String baseName = (name == null || name.trim().isEmpty()) ? "combo" : name.trim();
        String storedPath = fileStorageService.storeFile(file, "combos", baseName);
        return ResponseEntity.ok(Map.of("imagePath", storedPath));
    }
}