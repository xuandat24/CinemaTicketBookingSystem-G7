package com.G7.CTBS.service;

import com.G7.CTBS.dto.CategoryDTO;
import com.G7.CTBS.entity.Category;
import com.G7.CTBS.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    public List<CategoryDTO> findByCategoryName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return categoryRepository.findAll().stream().map(this::convertToDTO).toList();
        }
        List<Category> categoryList = categoryRepository.findByNameContainingIgnoreCase(categoryName);
        if (categoryList.isEmpty()) {
            return new ArrayList<>();
        }
        
        return categoryList.stream().map(this::convertToDTO).toList();
    }
    
    public CategoryDTO findById(Long id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            throw new EntityNotFoundException("Category with id " + id + " not found");
        }
        return convertToDTO(category);
    }
    
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll().stream().map(this::convertToDTO).toList();
    }
    
    public void createCategory(CategoryDTO categoryDTO) {
        if(categoryRepository.existsByNameIgnoreCase(categoryDTO.getName())) {
            throw  new EntityExistsException("Category with name " + categoryDTO.getName() + " already exists");
        }
        Category category = new Category();
        category.setName(categoryDTO.getName());
        
        categoryRepository.save(category);
    }
    
    public void updateCategory(CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(categoryDTO.getCategoryId()).orElse(null);
        if (category == null) {
            throw new EntityNotFoundException("Category not found");
        }
        
        if (categoryRepository.existsByNameIgnoreCase(categoryDTO.getName())) {
            throw  new EntityExistsException("Category with name " + categoryDTO.getName() + " already exists");
        }
        
        category.setName(categoryDTO.getName());
        categoryRepository.save(category);
    }
    
    public void deleteCategoryById(Long id) {
        if(!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category with id " + id + " not found");
        }
        try{
            categoryRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Category cannot be delete now. It might be linked to some movie.");
        }
    }
    
    public CategoryDTO convertToDTO(Category category) {
        long count = categoryRepository.countMoviesByCategoryId(category.getCategoryId());
        
        return CategoryDTO.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .movieCount(count)
                .build();
    }
}
