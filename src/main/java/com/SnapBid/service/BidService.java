package com.SnapBid.service;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.Bid;
import com.SnapBid.model.BidMessage;
import com.SnapBid.model.User;
import com.SnapBid.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionService auctionService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public BidService(BidRepository bidRepository, AuctionService auctionService, SimpMessagingTemplate messagingTemplate) {
        this.bidRepository = bidRepository;
        this.auctionService = auctionService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Bid placeBid(Auction auction, User bidder, BigDecimal amount) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new RuntimeException("Cannot place bid on inactive auction");
        }
        if (auction.getEndDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Auction has ended");
        }
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new RuntimeException("Bid amount must be higher than current price");
        }
        if (auction.getSeller().equals(bidder)) {
            throw new RuntimeException("Seller cannot bid on their own auction");
        }
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());

        auction.setCurrentPrice(amount);
        auctionService.updateAuction(auction.getId(), auction);

        // Save bid to database
        Bid savedBid = bidRepository.save(bid);
        
        // Send WebSocket notification about the new bid
        notifyBidUpdate(savedBid);
        
        return savedBid;
    }

    /**
     * Sends a WebSocket message to notify clients about a new bid
     */
    private void notifyBidUpdate(Bid bid) {
        BidMessage bidMessage = new BidMessage(
            bid.getAuction().getId(),
            bid.getAmount().doubleValue(),
            bid.getBidder().getUsername(),
            bid.getBidTime().format(FORMATTER),
            bid.getAmount().doubleValue() // Current highest bid is the same as this bid
        );
        
        // Send to topic specific to this auction
        messagingTemplate.convertAndSend(
            "/topic/auction/" + bid.getAuction().getId(), 
            bidMessage
        );
    }

    @Transactional(readOnly = true)
    public List<Bid> getBidsByAuction(Auction auction) {
        return bidRepository.findByAuctionOrderByBidTimeDesc(auction);
    }

    @Transactional(readOnly = true)
    public List<Bid> getBidsByUser(User user) {
        return bidRepository.findByBidderOrderByBidTimeDesc(user);
    }

    @Transactional(readOnly = true)
    public Bid getHighestBid(Auction auction) {
        return bidRepository.findTopByAuctionOrderByAmountDesc(auction)
            .orElse(null);
    }

    @Transactional
    public Bid createBid(Bid bid, User bidder) {
        // Validate auction is active
        if (bid.getAuction().getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot bid on an auction that is not active");
        }

        // Validate auction hasn't ended
        if (bid.getAuction().getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot bid on an auction that has ended");
        }

        // Validate bid amount is higher than current highest bid
        Bid highestBid = bidRepository.findTopByAuctionOrderByAmountDesc(bid.getAuction())
            .orElse(null);
        if (highestBid != null && bid.getAmount().compareTo(highestBid.getAmount()) <= 0) {
            throw new IllegalStateException("Bid amount must be higher than the current highest bid");
        }

        bid.setBidder(bidder);
        bid.setBidTime(LocalDateTime.now());
        return bidRepository.save(bid);
    }
} 