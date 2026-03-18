package com.G7.CTBS.controller;

import com.G7.CTBS.entity.User;
import com.G7.CTBS.service.ComboService;
import com.G7.CTBS.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BookingController {

    @Autowired
    private ComboService comboService;

    @Autowired
    private UserService userService;

    @GetMapping("/booking/{id}")
    public String booking(@PathVariable("id") Long id,
                          @RequestParam(value = "username", required = false) String username,
                          Model model) {

        // ❗ Nếu chưa login → đá về login
        if (username == null) {
            return "redirect:/login";
        }

        // 👉 Lấy user từ DB
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);

        // Fake movie
        Map<Long, Map<String, String>> movies = new HashMap<>();

        Map<String, String> movie1 = new HashMap<>();
        movie1.put("name", "The Semper Season 3");
        movie1.put("poster", "/img/1.jpg");
        movies.put(1L, movie1);

        Map<String, String> movie2 = new HashMap<>();
        movie2.put("name", "Doraemon");
        movie2.put("poster", "/img/2.jpg");
        movies.put(2L, movie2);

        Map<String, String> movie3 = new HashMap<>();
        movie3.put("name", "Inception");
        movie3.put("poster", "/img/3.jpg");
        movies.put(3L, movie3);

        Map<String, String> movie = movies.getOrDefault(id, movie1);

        model.addAttribute("movieName", movie.get("name"));
        model.addAttribute("poster", movie.get("poster"));

        model.addAttribute("combos", comboService.getAllCombos());

        return "booking";
    }
}