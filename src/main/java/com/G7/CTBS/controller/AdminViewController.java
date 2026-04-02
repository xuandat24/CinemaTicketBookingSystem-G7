package com.G7.CTBS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    @GetMapping("/profile")
    public String adminProfilePage() {
        return "admin/profile-admin";
    }

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

    @GetMapping("/showtimes/create")
    public String addShowtimesPage() {
        return "admin/create-showtime";
    }

    @GetMapping("/showtimes")
    public String searchShowtimesPage() {
        return "admin/search-showtime";
    }

    @GetMapping("/showtimes/update")
    public String updateShowtimesPage() {
        return "admin/update-showtime";
    }

    @GetMapping("/combos/add")
    public String addComboPage() {
        return "admin/addCombo";
    }

    @GetMapping("/combos")
    public String searchComboPage() {
        return "admin/searchCombo";
    }

    @GetMapping("/combos/edit")
    public String editComboPage() {
        return "admin/editCombo";
    }

    @GetMapping("/theaters")
    public String theaterManagementPage() {
        return "admin/theater-management";
    }
}
