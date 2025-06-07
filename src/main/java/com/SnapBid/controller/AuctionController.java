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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    public String listAuctions(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        // Explicitly remove the successMessage from the model to prevent persistence
        if (model.containsAttribute("successMessage")) {
            model.asMap().remove("successMessage");
        }

        List<Auction> auctions;
        if (keyword != null && !keyword.isEmpty()) {
            auctions = auctionService.searchAuctions(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            auctions = auctionService.getAllActiveAuctions();
        }
        model.addAttribute("auctions", auctions);

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
    public String viewAuction(@PathVariable("id") Long id, Model model) {
        Optional<Auction> auctionOptional = auctionService.getAuctionById(id);
        if (!auctionOptional.isPresent()) {
            return "redirect:/auctions";
        }
        Auction auction = auctionOptional.get();
        model.addAttribute("auction", auction);
        model.addAttribute("bid", new Bid());
        model.addAttribute("title", auction.getTitle() + " - Auction Details");

        // Calculate minimum bid amount
        BigDecimal minBidAmount = auction.getCurrentPrice();
        if (minBidAmount == null || minBidAmount.compareTo(auction.getStartingPrice()) < 0) {
            minBidAmount = auction.getStartingPrice();
        }
        minBidAmount = minBidAmount.add(new BigDecimal("0.01"));
        model.addAttribute("minBidAmount", minBidAmount);

        return "auction/detail";
    }

    @PostMapping("/{id}/bid")
    @Transactional
    public String placeBid(@PathVariable("id") Long auctionId, @RequestParam("amount") BigDecimal amount, @AuthenticationPrincipal User bidder, RedirectAttributes redirectAttributes) {
        try {
            Optional<Auction> auctionOptional = auctionService.getAuctionById(auctionId);
            if (!auctionOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Auction not found!");
                return "redirect:/auctions";
            }
            Auction auction = auctionOptional.get();
            auctionService.placeBid(auction, bidder, amount);
            redirectAttributes.addFlashAttribute("successMessage", "Bid placed successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to place bid: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to place bid: An unexpected error occurred.");
            System.err.println("Error placing bid: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/auctions/" + auctionId;
    }

    @GetMapping("/my-auctions")
    @Transactional(readOnly = true)
    public String myAuctions(Model model, @AuthenticationPrincipal User currentUser) {
        List<Auction> myAuctions = auctionService.getUserAuctions(currentUser);
        model.addAttribute("auctions", myAuctions);
        model.addAttribute("title", "My Auctions");
        return "auction/my-auctions";
    }

    @GetMapping("/my-bids")
    @Transactional(readOnly = true)
    public String myBids(Model model, @AuthenticationPrincipal User currentUser) {
        List<Auction> biddedAuctions = auctionService.getUserBids(currentUser);
        model.addAttribute("auctions", biddedAuctions);
        model.addAttribute("title", "My Bids");
        return "auction/my-bids";
    }

    @PostMapping("/{id}/end")
    @Transactional
    public String endAuction(@PathVariable("id") Long auctionId, RedirectAttributes redirectAttributes) {
        try {
            Optional<Auction> auctionOptional = auctionService.getAuctionById(auctionId);
            if (!auctionOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Auction not found!");
                return "redirect:/auctions";
            }
            Auction auction = auctionOptional.get();
            auctionService.endAuction(auction);
            redirectAttributes.addFlashAttribute("successMessage", "Auction ended successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to end auction: " + e.getMessage());
        }
        return "redirect:/auctions/" + auctionId;
    }
} 