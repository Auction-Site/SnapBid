package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import com.SnapBid.service.CategoryService;
import com.SnapBid.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auctions")
@Transactional
public class AuctionController {

    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;
    private final UserService userService;
    private final CategoryService categoryService;

    @Value("${app.upload.dir}")
    private String uploadDir;

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
            if (auctions.isEmpty()) {
                model.addAttribute("infoMessage", "No auctions found matching '" + keyword + "'");
            } else {
                model.addAttribute("infoMessage", "Found " + auctions.size() + " auctions matching '" + keyword + "'");
            }
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
                              @RequestParam("image") MultipartFile image,
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
            // Handle image upload
            if (image != null && !image.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                File uploadPath = new File(uploadDir);
                if (!uploadPath.exists()) {
                    uploadPath.mkdirs();
                }
                
                File destFile = new File(uploadPath.getAbsolutePath() + File.separator + fileName);
                image.transferTo(destFile);
                
                // Set the image URL in the auction
                auction.setImageUrl(fileName);
                logger.info("Image uploaded successfully: {}", fileName);
            }

            Auction savedAuction = auctionService.createAuction(auction, seller, bindingResult);
            if (savedAuction != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Auction created successfully!");
                return "redirect:/auctions/" + savedAuction.getId() + "?new=true";
            } else {
                model.addAttribute("error", "Failed to create auction");
                model.addAttribute("categories", categoryService.getAllCategories());
                return "auction/create";
            }
        } catch (IOException e) {
            logger.error("Error uploading image: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to upload image: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "auction/create";
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
            logger.info("Attempting to end auction with ID: {}", auctionId);
            
            Optional<Auction> auctionOptional = auctionService.getAuctionById(auctionId);
            if (!auctionOptional.isPresent()) {
                logger.warn("Auction not found with ID: {}", auctionId);
                redirectAttributes.addFlashAttribute("errorMessage", "Auction not found!");
                return "redirect:/auctions";
            }
            
            Auction auction = auctionOptional.get();
            logger.info("Found auction: id={}, status={}, seller={}", 
                auction.getId(), 
                auction.getStatus(), 
                auction.getSeller().getUsername());
            
            auctionService.endAuction(auction);
            logger.info("Successfully ended auction: id={}", auctionId);
            
            redirectAttributes.addFlashAttribute("successMessage", "Auction ended successfully!");
        } catch (Exception e) {
            logger.error("Failed to end auction: id={}, error={}", auctionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to end auction: " + e.getMessage());
        }
        return "redirect:/auctions/" + auctionId;
    }

    @GetMapping("/{id}/edit")
    @Transactional(readOnly = true)
    public String showEditForm(@PathVariable("id") Long id, Model model, @AuthenticationPrincipal User currentUser) {
        Optional<Auction> auctionOptional = auctionService.getAuctionById(id);
        if (!auctionOptional.isPresent()) {
            return "redirect:/auctions";
        }
        
        Auction auction = auctionOptional.get();
        
        // Check if the current user is the seller
        if (!currentUser.getId().equals(auction.getSeller().getId())) {
            return "redirect:/auctions/" + id;
        }
        
        model.addAttribute("auction", auction);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Edit Auction - " + auction.getTitle());
        
        return "auction/edit";
    }

    @PostMapping("/{id}/edit")
    @Transactional
    public String updateAuction(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("auction") Auction auctionDetails,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User currentUser,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Optional<Auction> auctionOptional = auctionService.getAuctionById(id);
        if (!auctionOptional.isPresent()) {
            return "redirect:/auctions";
        }
        
        Auction existingAuction = auctionOptional.get();
        
        // Check if the current user is the seller
        if (!currentUser.getId().equals(existingAuction.getSeller().getId())) {
            return "redirect:/auctions/" + id;
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "auction/edit";
        }
        
        try {
            Auction updatedAuction = auctionService.updateAuction(id, auctionDetails);
            if (updatedAuction != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Auction updated successfully!");
                return "redirect:/auctions/" + id;
            } else {
                model.addAttribute("error", "Failed to update auction");
                model.addAttribute("categories", categoryService.getAllCategories());
                return "auction/edit";
            }
        } catch (Exception e) {
            logger.error("Error updating auction: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while updating the auction");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "auction/edit";
        }
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteAuction(@PathVariable("id") Long id,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            Optional<Auction> auctionOptional = auctionService.getAuctionById(id);
            if (!auctionOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Auction not found!");
                return "redirect:/auctions";
            }
            
            Auction auction = auctionOptional.get();
            
            // Check if the current user is the seller
            if (!currentUser.getId().equals(auction.getSeller().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to delete this auction!");
                return "redirect:/auctions/" + id;
            }
            
            auctionService.deleteAuction(id);
            redirectAttributes.addFlashAttribute("successMessage", "Auction deleted successfully!");
            return "redirect:/auctions";
        } catch (Exception e) {
            logger.error("Error deleting auction: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete auction: " + e.getMessage());
            return "redirect:/auctions/" + id;
        }
    }
} 