package com.G7.CTBS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BookingController {

    @GetMapping("/booking/{id}")
    public String booking(@PathVariable("id") Long id, Model model) {

        // Map giả lập movieId -> movie info
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

        // Lấy thông tin theo id, nếu id không tồn tại thì trả movie1
        Map<String, String> movie = movies.getOrDefault(id, movie1);

        model.addAttribute("movieName", movie.get("name"));
        model.addAttribute("poster", movie.get("poster"));

        return "booking"; // render booking.html
    }
}