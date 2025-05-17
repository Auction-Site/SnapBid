package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.Bid;
import com.SnapBid.model.BidMessage;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import com.SnapBid.service.BidService;
import com.SnapBid.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
public class BidRestController {
    private static final Logger logger = LoggerFactory.getLogger(BidRestController.class);
    private final AuctionService auctionService;
    private final UserService userService;
    private final BidService bidService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BidRestController(AuctionService auctionService, UserService userService, BidService bidService) {
        this.auctionService = auctionService;
        this.userService = userService;
        this.bidService = bidService;
    }

    /**
     * REST endpoint for placing a bid via AJAX
     */
    @PostMapping("/{auctionId}")
    public ResponseEntity<?> placeBid(@PathVariable Long auctionId, @RequestBody Map<String, Object> payload) {
        logger.info("Received bid for auction {}: {}", auctionId, payload);
        
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // Check if user is authenticated
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("Unauthorized bid attempt for auction {}", auctionId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "You must be logged in to place a bid"));
            }
            
            String username = auth.getName();
            logger.info("Processing bid from user: {}", username);
            
            // Get user
            User bidder = userService.findByUsername(username);
            
            // Get auction
            Auction auction = auctionService.findById(auctionId);
            if (auction == null) {
                logger.warn("Auction not found: {}", auctionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Auction not found"));
            }
            
            // Ensure the amount field exists in the payload
            if (!payload.containsKey("amount")) {
                logger.warn("No amount specified in bid payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Bid amount is required"));
            }
            
            // Extract and validate bid amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(payload.get("amount").toString());
            } catch (NumberFormatException e) {
                logger.warn("Invalid bid amount format: {}", payload.get("amount"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid bid amount format"));
            }
            
            // Place bid
            Bid bid = bidService.placeBid(auction, bidder, amount);
            
            // Create response object
            BidMessage response = new BidMessage(
                auction.getId(),
                bid.getAmount().doubleValue(),
                bidder.getUsername(),
                LocalDateTime.now().format(FORMATTER),
                auction.getCurrentPrice().doubleValue()
            );
            
            logger.info("Bid placed successfully: auction={}, user={}, amount={}", 
                    auctionId, username, amount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing bid: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
} 