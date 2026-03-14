package com.G7.CTBS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {
    
    @GetMapping({"", "/", "/home"})
    public String homePage() {
        return "admin/home";
    }
    
    @GetMapping("/movies/search")
    public String searchPage() {
        return "admin/searchMovie";
    }
    
    @GetMapping("/movies/add")
    public String addPage() {
        return "admin/addMovie";
    }
    
    @GetMapping("/movies/edit")
    public String editPage() {
        return "admin/editMovie";
    }
    
    @GetMapping("/categories")
    public String searchCategoryPage() {
        return "admin/searchCategory";
    }
    
    @GetMapping("/categories/add")
    public String addCategoryPage() {
        return "admin/addCategory";
    }
    
    @GetMapping("/categories/edit")
    public String editCategoryPage() {
        return "admin/editCategory";
    }
}