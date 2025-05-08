package com.SnapBid.controller;

import com.SnapBid.model.User;
import com.SnapBid.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WebController {

    private final UserService userService;

    public WebController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser");
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("title", "Register");
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Register");
            return "auth/register";
        }

        try {
            userService.registerUser(user);
            return "redirect:/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "Register");
            return "auth/register";
        }
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("title", "Create Auction");
        return "create";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("title", "Profile");
        return "profile";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy Policy");
        return "privacy";
    }
} 