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
    @GetMapping("/")
    public String home() {
        return "index";
    }
}