package com.SnapBid.controller;

import com.SnapBid.model.User;
import com.SnapBid.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
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
        logger.info("Showing registration form");
        model.addAttribute("title", "Register");
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, 
                               BindingResult result, 
                               Model model,
                               RedirectAttributes redirectAttributes) {
        logger.info("Processing registration for user: {}", user.getUsername());
        
        if (result.hasErrors()) {
            logger.warn("Registration form has errors: {}", result.getAllErrors());
            model.addAttribute("title", "Register");
            return "auth/register";
        }

        try {
            userService.registerUser(user);
            logger.info("User successfully registered: {}", user.getUsername());
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (RuntimeException e) {
            logger.error("Error during registration: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "Register");
            return "auth/register";
        }
    }

    @GetMapping("/create")
    public String create(Model model) {
        logger.info("Processing create request");
        
        // Check if user is authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            logger.warn("Unauthenticated user attempting to access /create");
            return "redirect:/login?error=notAuthenticated";
        }
        
        logger.info("Redirecting to auction creation form");
        model.addAttribute("title", "Create Auction");
        return "redirect:/auctions/create";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About Us");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Contact Us");
        return "contact";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("title", "Terms & Conditions");
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy Policy");
        return "privacy";
    }
} 