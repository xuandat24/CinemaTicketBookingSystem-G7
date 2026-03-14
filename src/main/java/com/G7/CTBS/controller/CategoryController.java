package com.G7.CTBS.controller;

import com.G7.CTBS.dto.CategoryDTO;
import com.G7.CTBS.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategories(@RequestParam(required = false) String name) {
        if (name != null && !name.trim().isEmpty()) {
            return ResponseEntity.ok(categoryService.findByCategoryName(name));
        }
        return ResponseEntity.ok(categoryService.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.createCategory(categoryDTO);
        return ResponseEntity.ok(Map.of("message", "Category added successfully!"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO categoryDTO) {
        categoryDTO.setCategoryId(id);
        categoryService.updateCategory(categoryDTO);
        return ResponseEntity.ok(Map.of("message", "Category updated successfully!"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategoryById(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully!"));
    }
}