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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/auctions")
public class AuctionController {

    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;
    private final UserService userService;
    private final CategoryService categoryService;

    public AuctionController(AuctionService auctionService, UserService userService, CategoryService categoryService) {
        this.auctionService = auctionService;
        this.userService = userService;
        this.categoryService = categoryService;
    }

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
            auctions = auctionService.getActiveAuctions();
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
        if (!model.containsAttribute("auction")) {
            Auction auction = new Auction();
            auction.setStartDate(LocalDateTime.now());
            auction.setEndDate(LocalDateTime.now().plusDays(7));
            model.addAttribute("auction", auction);
        }
        
        // Add category list to the model
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("title", "Create Auction");
        
        return "auction/create";
    }

    @PostMapping("/create")
    public String createAuction(@Valid @ModelAttribute("auction") Auction auction,
                              BindingResult result,
                              @RequestParam(required = false, defaultValue = "false") boolean showInList,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Add category list to the model for validation errors
        if (result.hasErrors()) {
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("title", "Create Auction");
            return "auction/create";
        }

        try {
            // Get authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return "redirect:/login?error=notAuthenticated";
            }
            
            User seller = userService.findByUsername(auth.getName());

            // Set startDate to current time if not set
            if (auction.getStartDate() == null) {
                auction.setStartDate(LocalDateTime.now());
            }
            
            // Set current price to starting price initially
            auction.setCurrentPrice(auction.getStartingPrice());
            
            // Set auction status to ACTIVE
            auction.setStatus(AuctionStatus.ACTIVE);
            
            // Save the auction
            Auction savedAuction = auctionService.createAuction(auction, seller);
            
            // Add success message
            String successMessage = "Your auction '" + savedAuction.getTitle() + "' has been created successfully!";
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            
            // Redirect based on showInList parameter
            if (showInList) {
                redirectAttributes.addAttribute("newAuctionId", savedAuction.getId());
                return "redirect:/auctions";
            } else {
                // Redirect to the newly created auction detail page with new=true parameter
                return "redirect:/auctions/" + savedAuction.getId() + "?new=true";
            }
        } catch (RuntimeException e) {
            // Add category list to the model when there are errors
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "Create Auction");
            return "auction/create";
        }
    }

    @GetMapping("/{id}")
    public String viewAuction(@PathVariable Long id, 
                            @RequestParam(value = "new", required = false) Boolean isNew,
                            Model model) {
        logger.info("Loading auction details page for auction ID: {}", id);
        
        try {
            Auction auction = auctionService.findById(id);
            model.addAttribute("auction", auction);
            model.addAttribute("bid", new Bid());
            model.addAttribute("title", auction.getTitle() + " - Auction Details");
            
            if (Boolean.TRUE.equals(isNew)) {
                model.addAttribute("successMessage", "Your auction has been created successfully!");
            }
            
            return "auction/detail";
        } catch (Exception e) {
            logger.error("Error loading auction {}: {}", id, e.getMessage());
            model.addAttribute("error", "Auction not found");
            return "redirect:/auctions";
        }
    }

    @PostMapping("/{id}/bid")
    public String placeBid(@PathVariable Long id,
                          @Valid @ModelAttribute("bid") Bid bid,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Auction auction = auctionService.findById(id);
            model.addAttribute("auction", auction);
            return "auction/detail";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User bidder = userService.findByUsername(auth.getName());
        Auction auction = auctionService.findById(id);

        try {
            auctionService.placeBid(auction, bidder, bid.getAmount());
            redirectAttributes.addFlashAttribute("successMessage", "Your bid has been placed successfully!");
            return "redirect:/auctions/" + id;
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("auction", auction);
            return "auction/detail";
        }
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