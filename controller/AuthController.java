package com.wsims.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/")
    public String home() {
        return "index"; // Renders index.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Renders login.html
    }
}