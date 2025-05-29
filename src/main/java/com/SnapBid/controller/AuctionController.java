package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.Bid;
import com.SnapBid.model.Category;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import com.SnapBid.service.CategoryService;
import com.SnapBid.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/auctions")
public class AuctionController {

    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String listAuctions(Model model, 
                             @RequestParam(required = false) Long newAuctionId,
                             @RequestParam(required = false) String keyword,
                             @ModelAttribute("successMessage") String successMessage) {
        logger.info("Loading auction listings page. Keyword: {}, NewAuctionId: {}", keyword, newAuctionId);
        
        List<Auction> auctions;
        
        if (keyword != null && !keyword.isEmpty()) {
            auctions = auctionService.searchAuctions(keyword);
            model.addAttribute("keyword", keyword);
            logger.info("Search results for '{}': {} auctions found", keyword, auctions.size());
        } else {
            auctions = auctionService.getAllActiveAuctions();
            logger.info("Loading all active auctions: {} found", auctions.size());
        }
        
        model.addAttribute("auctions", auctions);
        model.addAttribute("title", "Browse Auctions");
        
        if (newAuctionId != null) {
            model.addAttribute("newAuctionId", newAuctionId);
        }
        
        return "auction/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        logger.info("Showing auction creation form");
        model.addAttribute("auction", new Auction());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Create Auction");
        
        return "auction/create";
    }

    @PostMapping("/create")
    public String createAuction(@Valid @ModelAttribute("auction") Auction auction,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        logger.info("Received auction creation request from user: {}", currentUser.getUsername());
        
        if (currentUser == null || currentUser.getId() == null) {
            logger.error("User not properly authenticated or not found in database");
            redirectAttributes.addFlashAttribute("errorMessage", 
                "You must be logged in to create an auction.");
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            logger.warn("Auction creation form validation failed for user {}: {}", 
                currentUser.getUsername(),
                bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining(", ")));
            return "auction/create";
        }

        try {
            User seller = userService.findByUsername(currentUser.getUsername());
            if (seller == null || seller.getId() == null) {
                logger.error("User not found in database: {}", currentUser.getUsername());
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "User account not found. Please try logging in again.");
                return "redirect:/login";
            }
            
            Auction createdAuction = auctionService.createAuction(auction, seller, bindingResult);
            
            if (createdAuction != null) {
                logger.info("Auction created successfully: id={}, title={}", 
                    createdAuction.getId(), 
                    createdAuction.getTitle());
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Auction created successfully!");
                return "redirect:/auctions";
            } else {
                logger.warn("Auction creation failed for user {}: validation errors", 
                    currentUser.getUsername());
                return "auction/create";
            }
        } catch (Exception e) {
            logger.error("Error creating auction for user {}: {}", 
                currentUser.getUsername(), 
                e.getMessage(), 
                e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to create auction. Please try again.");
            return "redirect:/auctions/create";
        }
    }

    @GetMapping("/{id}")
    public String viewAuction(@PathVariable Long id, 
                            @RequestParam(value = "new", required = false) Boolean isNew,
                            Model model) {
        logger.info("Loading auction details page for auction ID: {}", id);
        
        return auctionService.getAuctionById(id)
                .map(auction -> {
                    model.addAttribute("auction", auction);
                    model.addAttribute("bid", new Bid());
                    model.addAttribute("title", auction.getTitle() + " - Auction Details");
                    
                    if (Boolean.TRUE.equals(isNew)) {
                        model.addAttribute("successMessage", "Your auction has been created successfully!");
                    }
                    
                    return "auction/detail";
                })
                .orElse("redirect:/auctions");
    }

    @PostMapping("/{id}/bid")
    @ResponseBody
    public String placeBid(@PathVariable Long id,
                          @RequestParam BigDecimal amount,
                          @AuthenticationPrincipal User bidder,
                          RedirectAttributes redirectAttributes) {
        try {
            Auction auction = auctionService.getAuctionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

            auctionService.placeBid(auction, bidder, amount);
            return "success";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @PostMapping("/{id}/end")
    public String endAuction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Auction auction = auctionService.getAuctionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        auctionService.endAuction(auction);
        redirectAttributes.addFlashAttribute("successMessage", "Auction ended successfully");
        return "redirect:/auctions/" + id;
    }

    @GetMapping("/my-auctions")
    public String myAuctions(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        
        List<Auction> myAuctions = auctionService.getUserAuctions(user);
        model.addAttribute("auctions", myAuctions);
        return "auction/my-auctions";
    }

    @GetMapping("/my-bids")
    public String myBids(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        
        List<Auction> myBids = auctionService.getUserBids(user);
        model.addAttribute("auctions", myBids);
        return "auction/my-bids";
    }
} 