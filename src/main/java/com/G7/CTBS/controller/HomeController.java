package com.G7.CTBS.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("currentPage", "about");
        return "about";
    }

    @GetMapping("/login")
    public String login() {
        return "user/login";
    }

    @GetMapping("/register")
    public String register() {
        return "user/register";
    }

    @GetMapping("/")
    public String viewHomePage(Model model) {
        model.addAttribute("currentPage", "home");
        return "index";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage() {
        return "verify-otp";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("currentPage", "profile"); // Không có mục menu chính, nhưng thêm để thống nhất
        return "user/profile";
    }

    @GetMapping("/movies")
    public String moviesPage(Model model) {
        model.addAttribute("currentPage", "movies");
        return "movies";
    }

    @GetMapping("/detail")
    public String movieDetailPage(Model model) {
        // Giữ cho mục "Movies" vẫn sáng đèn khi xem chi tiết
        model.addAttribute("currentPage", "movies");
        return "detail";
    }

}