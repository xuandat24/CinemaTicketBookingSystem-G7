package com.G7.CTBS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/about")
    public String about() {
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
    public String home() {
        return "index";
    }
    
    @GetMapping("/verify-otp")
    public String verifyOtpPage() {
        return "verify-otp"; // Trả về file verify-otp.html trong thư mục templates
    }
    
    @GetMapping("/movie-seats")
    public String movieSeats() {
        return "movie-seats";
    }
    
    @GetMapping("/movies")
    public String moviesPage() {
        // Trả về file movies.html trong thư mục templates
        return "movies";
    }
    
    @GetMapping("/detail")
    public String movieDetailPage() {
        // Trả về file detail.html trong thư mục templates
        return "detail";
    }
}
