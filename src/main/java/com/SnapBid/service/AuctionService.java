package com.SnapBid.service;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.repository.AuctionRepository;
import com.SnapBid.repository.BidRepository;
import com.SnapBid.validation.OnCreate;
import com.SnapBid.websocket.WebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WebSocketController webSocketController;

    public AuctionService(AuctionRepository auctionRepository,
                         BidRepository bidRepository,
                         WebSocketController webSocketController) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.webSocketController = webSocketController;
    }

    @Transactional(readOnly = true)
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Auction> getAuctionById(Long id) {
        return auctionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Auction> getAllActiveAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE);
    }

    @Transactional
    public Auction createAuction(Auction auction, User seller, BindingResult bindingResult) {
        logger.info("Starting auction creation process for seller: {}", seller.getUsername());
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            if (auction.getStartDate() == null || auction.getStartDate().isBefore(now)) {
                auction.setStartDate(now);
            }
            
            auction.setCreatedAt(now);
            auction.setCurrentPrice(auction.getStartingPrice());
            
            // Validate auction data for creation group
            validateAuction(auction, bindingResult, OnCreate.class);
            if (bindingResult.hasErrors()) {
                logger.warn("Auction validation failed for seller {}: {}", 
                    seller.getUsername(), 
                    bindingResult.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(java.util.stream.Collectors.joining(", ")));
                return null;
            }

            // Set remaining auction properties
            auction.setSeller(seller);
            auction.setStatus(AuctionStatus.ACTIVE);

            // Validate end date
            if (auction.getEndDate().isBefore(auction.getStartDate())) {
                logger.warn("Invalid end date for auction by seller {}: end date is before start date", seller.getUsername());
                bindingResult.addError(new FieldError("auction", "endDate", 
                    "End date must be after start date"));
                return null;
            }

            // Save auction
            logger.info("Saving auction to database: title={}, seller={}, startingPrice={}", 
                auction.getTitle(), 
                seller.getUsername(), 
                auction.getStartingPrice());
            
            Auction savedAuction = auctionRepository.save(auction);
            
            logger.info("Auction saved successfully: id={}, title={}", 
                savedAuction.getId(), 
                savedAuction.getTitle());

            // Notify all connected clients about the new auction
            webSocketController.notifyNewAuction(savedAuction);
            
            logger.info("Auction creation completed successfully: id={}", savedAuction.getId());
            return savedAuction;
            
        } catch (Exception e) {
            logger.error("Error creating auction for seller {}: {}", 
                seller.getUsername(), 
                e.getMessage(), 
                e);
            throw new RuntimeException("Failed to create auction", e);
        }
    }

    @Transactional
    public Auction updateAuction(Long id, Auction auctionDetails) {
        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isPresent()) {
            Auction auction = optionalAuction.get();
            auction.setTitle(auctionDetails.getTitle());
            auction.setDescription(auctionDetails.getDescription());
            auction.setStartingPrice(auctionDetails.getStartingPrice());
            auction.setCurrentPrice(auctionDetails.getCurrentPrice());
            auction.setStartDate(auctionDetails.getStartDate());
            auction.setEndDate(auctionDetails.getEndDate());
            auction.setUpdatedAt(LocalDateTime.now());
            return auctionRepository.save(auction);
        }
        return null;
    }

    @Transactional
    public void deleteAuction(Long id) {
        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isPresent()) {
            Auction auction = optionalAuction.get();
            
            bidRepository.deleteByAuction(auction);
            auctionRepository.delete(auction);
            
            logger.info("Auction and associated bids deleted successfully: id={}", id);
        } else {
            logger.warn("Attempted to delete non-existent auction: id={}", id);
            throw new RuntimeException("Auction not found");
        }
    }

    @Transactional(readOnly = true)
    public List<Auction> getActiveAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Auction> getEndedAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ENDED);
    }

    @Transactional
    public void endAuction(Auction auction) {
        try {
            logger.info("Starting auction end process for auction: id={}", auction.getId());
            
            // Get the highest bid
            Optional<Bid> highestBidOpt = bidRepository.findTopByAuctionOrderByAmountDesc(auction);
            logger.info("Found highest bid for auction: id={}, hasBid={}", 
                auction.getId(), 
                highestBidOpt.isPresent());
            
            // Set the winner if there are any bids
            if (highestBidOpt.isPresent()) {
                Bid highestBid = highestBidOpt.get();
                auction.setWinner(highestBid.getBidder());
                logger.info("Set winner for auction: id={}, winner={}", 
                    auction.getId(), 
                    highestBid.getBidder().getUsername());
            }
            
            auction.setStatus(AuctionStatus.ENDED);
            auction.setUpdatedAt(LocalDateTime.now());
            
            // Save the auction
            Auction savedAuction = auctionRepository.save(auction);
            logger.info("Successfully saved ended auction: id={}, status={}", 
                savedAuction.getId(), 
                savedAuction.getStatus());
            
        } catch (Exception e) {
            logger.error("Error ending auction: id={}, error={}", 
                auction.getId(), 
                e.getMessage(), 
                e);
            throw new RuntimeException("Failed to end auction: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelAuction(Long id) {
        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isPresent()) {
            Auction auction = optionalAuction.get();
            auction.setStatus(AuctionStatus.CANCELLED);
            auction.setUpdatedAt(LocalDateTime.now());
            auctionRepository.save(auction);
        }
    }

    @Transactional
    public Auction placeBid(Auction auction, User bidder, BigDecimal amount) {
        // Validate bid
        validateBid(auction, bidder, amount);

        // Create and save bid
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bidRepository.save(bid);

        // Update auction
        auction.setCurrentPrice(amount);
        auction.setUpdatedAt(LocalDateTime.now());

        // Save and return updated auction
        return auctionRepository.save(auction);
    }

    @Transactional(readOnly = true)
    public List<Auction> getUserAuctions(User user) {
        return auctionRepository.findBySeller(user);
    }

    @Transactional(readOnly = true)
    public List<Auction> getUserBids(User user) {
        return auctionRepository.findByBids_Bidder(user);
    }

    @Transactional(readOnly = true)
    public Auction findById(Long id) {
        return auctionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Auction not found"));
    }

    @Transactional(readOnly = true)
    public List<Auction> findActiveAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Auction> findAuctionsByCategory(Long categoryId) {
        return auctionRepository.findByCategory_Id(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Auction> findAuctionsBySeller(Long sellerId) {
        return auctionRepository.findBySeller_Id(sellerId);
    }

    @Transactional(readOnly = true)
    public List<Auction> searchAuctions(String keyword) {
        logger.info("Searching auctions with keyword: {}", keyword);
        List<Auction> results = auctionRepository.searchByKeyword(keyword);
        logger.info("Found {} auctions matching keyword: {}", results.size(), keyword);
        return results;
    }

    @Transactional(readOnly = true)
    public List<Auction> findAuctionsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return auctionRepository.findByPriceRange(minPrice, maxPrice);
    }

    @Transactional
    public void endExpiredAuctions() {
        List<Auction> expiredAuctions = auctionRepository.findByEndDateBeforeAndStatus(
            LocalDateTime.now(), 
            AuctionStatus.ACTIVE
        );
        for (Auction auction : expiredAuctions) {
            auction.setStatus(AuctionStatus.ENDED);
            auctionRepository.save(auction);
        }
    }

    @Transactional(readOnly = true)
    public List<Auction> getUserWonAuctions(User user) {
        return bidRepository.findByBidderAndAuctionStatus(user, AuctionStatus.ENDED).stream()
            .map(Bid::getAuction)
            .filter(auction -> auction.getWinner() != null && auction.getWinner().equals(user))
            .distinct()
            .collect(Collectors.toList());
    }

    // Method for common auction validation
    private void validateAuction(Auction auction, BindingResult bindingResult, Class<?>... groups) {
        logger.debug("Validating auction: title={}", auction.getTitle());
        
        // Validate title
        if (auction.getTitle() == null || auction.getTitle().trim().length() < 3) {
            logger.warn("Invalid auction title: {}", auction.getTitle());
            bindingResult.addError(new FieldError("auction", "title", 
                "Title must be at least 3 characters long"));
        }

        // Validate description
        if (auction.getDescription() == null || auction.getDescription().trim().length() < 10) {
            logger.warn("Invalid auction description length: {}", 
                auction.getDescription() != null ? auction.getDescription().length() : 0);
            bindingResult.addError(new FieldError("auction", "description", 
                "Description must be at least 10 characters long"));
        }

        // Validate starting price
        if (auction.getStartingPrice() == null || auction.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid starting price: {}", auction.getStartingPrice());
            bindingResult.addError(new FieldError("auction", "startingPrice", 
                "Starting price must be greater than 0"));
        }

        // Validate category
        if (auction.getCategory() == null) {
            logger.warn("Missing category for auction");
            bindingResult.addError(new FieldError("auction", "category", 
                "Category is required"));
        }

        // Validate end date
        if (auction.getEndDate() == null) {
            logger.warn("Missing end date for auction");
            bindingResult.addError(new FieldError("auction", "endDate", 
                "End date is required"));
        }
        
        logger.debug("Auction validation completed with {} errors", bindingResult.getErrorCount());
    }

    private void validateBid(Auction auction, User bidder, BigDecimal amount) {
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("Bid amount must be higher than current price");
        }

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot bid on inactive auction");
        }

        if (auction.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction has ended");
        }

        if (auction.getSeller().equals(bidder)) {
            throw new IllegalArgumentException("Seller cannot bid on their own auction");
        }
    }

    private Bid getHighestBid(Auction auction) {
        return bidRepository.findTopByAuctionOrderByAmountDesc(auction)
            .orElse(null);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void checkAndEndExpiredAuctions() {
        logger.info("Checking for expired auctions...");
        List<Auction> activeAuctions = auctionRepository.findByStatus(AuctionStatus.ACTIVE);
        
        for (Auction auction : activeAuctions) {
            if (auction.getEndDate().isBefore(LocalDateTime.now())) {
                logger.info("Ending expired auction: id={}", auction.getId());
                endAuction(auction);
            }
        }
    }
} 