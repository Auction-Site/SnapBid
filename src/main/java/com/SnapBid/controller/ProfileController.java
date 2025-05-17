package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import com.SnapBid.service.BidService;
import com.SnapBid.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public ProfileController(UserService userService, AuctionService auctionService, BidService bidService) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    @GetMapping
    public String profile(Model model) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If not authenticated, redirect to login
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login?error=notAuthenticated";
        }
        
        try {
            User user = userService.findByUsername(auth.getName());
            
            // Get user's auctions
            List<Auction> userAuctions = auctionService.getUserAuctions(user);
            
            // Get user's bids
            List<Auction> biddedAuctions = auctionService.getUserBids(user);
            
            // Get user's won auctions
            List<Auction> wonAuctions = auctionService.getUserWonAuctions(user);
            
            // Add to model
            model.addAttribute("user", user);
            model.addAttribute("userAuctions", userAuctions);
            model.addAttribute("biddedAuctions", biddedAuctions);
            model.addAttribute("wonAuctions", wonAuctions);
            model.addAttribute("title", "Profile");
            
            return "profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/login?error=profile";
        }
    }
} 