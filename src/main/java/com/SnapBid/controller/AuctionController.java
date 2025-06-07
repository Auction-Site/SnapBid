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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/auctions")
@Transactional
public class AuctionController {

    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;
    private final UserService userService;
    private final CategoryService categoryService;

    @Autowired
    public AuctionController(AuctionService auctionService, UserService userService, CategoryService categoryService) {
        this.auctionService = auctionService;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public String showCreateForm(Model model) {
        logger.info("Showing auction creation form");
        model.addAttribute("auction", new Auction());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Create Auction");
        
        return "auction/create";
    }

    @PostMapping("/create")
    @Transactional
    public String createAuction(@Valid @ModelAttribute("auction") Auction auction,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User seller,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        logger.info("Processing auction creation request from seller: {}", seller.getUsername());
        
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in auction creation form");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "auction/create";
        }

        try {
            Auction savedAuction = auctionService.createAuction(auction, seller, bindingResult);
            if (savedAuction != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Auction created successfully!");
                return "redirect:/auctions/" + savedAuction.getId() + "?new=true";
            } else {
                model.addAttribute("error", "Failed to create auction");
                model.addAttribute("categories", categoryService.getAllCategories());
                return "auction/create";
            }
        } catch (Exception e) {
            logger.error("Error creating auction: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while creating the auction");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "auction/create";
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
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
    @Transactional
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

    @GetMapping("/my-auctions")
    @Transactional(readOnly = true)
    public String myAuctions(Model model, @AuthenticationPrincipal User user) {
        List<Auction> userAuctions = auctionService.getUserAuctions(user);
        model.addAttribute("auctions", userAuctions);
        model.addAttribute("title", "My Auctions");
        return "auction/my-auctions";
    }

    @GetMapping("/my-bids")
    @Transactional(readOnly = true)
    public String myBids(Model model, @AuthenticationPrincipal User user) {
        List<Auction> biddedAuctions = auctionService.getUserBids(user);
        model.addAttribute("auctions", biddedAuctions);
        model.addAttribute("title", "My Bids");
        return "auction/my-bids";
    }

    @PostMapping("/{id}/end")
    @Transactional
    public String endAuction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Auction auction = auctionService.getAuctionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
            
            auctionService.endAuction(auction);
            redirectAttributes.addFlashAttribute("successMessage", "Auction ended successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auctions/" + id;
    }
} 