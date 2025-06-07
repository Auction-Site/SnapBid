package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    
    @Autowired
    private AuctionService auctionService;

    @GetMapping("/")
    public String home(Model model) {
        logger.info("Loading home page");
        
        try {
            // Get featured/recent auctions for the homepage
            List<Auction> featuredAuctions = auctionService.getActiveAuctions();
            model.addAttribute("featuredAuctions", featuredAuctions);
            model.addAttribute("title", "Home - Online Auction Platform");
            return "index";
        } catch (Exception e) {
            logger.error("Error loading home page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading homepage data");
            return "index";
        }
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           @RequestParam(value = "registered", required = false) String registered,
                           @RequestParam(value = "expired", required = false) String expired,
                           @RequestParam(value = "denied", required = false) String denied,
                           Model model) {
        
        logger.info("Processing login page request. Error: {}, Logout: {}, Registered: {}, Expired: {}, Denied: {}",
                error, logout, registered, expired, denied);
        
        model.addAttribute("title", "Login");
        model.addAttribute("user", new User());
        
        if (error != null) {
            if ("notAuthenticated".equals(error)) {
                model.addAttribute("errorMsg", "You must be logged in to access this page");
            } else if ("profile".equals(error)) {
                model.addAttribute("errorMsg", "There was an error accessing your profile");
            } else {
                model.addAttribute("errorMsg", "Invalid username or password. Please try again.");
            }
            logger.info("Login error: {}", error);
        }
        
        if (logout != null) {
            model.addAttribute("logoutMsg", "You have been logged out successfully.");
            logger.info("User has logged out");
        }
        
        if (registered != null) {
            model.addAttribute("registeredMsg", "Registration successful! Please login with your credentials.");
            logger.info("User has registered");
        }
        
        if (expired != null) {
            model.addAttribute("expiredMsg", "Your session has expired. Please login again.");
            logger.info("Session has expired");
        }
        
        if (denied != null) {
            model.addAttribute("deniedMsg", "Access denied. You don't have permission to access this resource.");
            logger.info("Access denied");
        }
        
        return "auth/login";
    }
}
