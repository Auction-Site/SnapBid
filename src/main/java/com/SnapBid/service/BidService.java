package com.SnapBid.service;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionService auctionService;

    @Autowired
    public BidService(BidRepository bidRepository, AuctionService auctionService) {
        this.bidRepository = bidRepository;
        this.auctionService = auctionService;
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

        return bidRepository.save(bid);
    }

    public List<Bid> getBidsByAuction(Auction auction) {
        return bidRepository.findByAuctionOrderByBidTimeDesc(auction);
    }

    public List<Bid> getBidsByUser(User user) {
        return bidRepository.findByBidderOrderByBidTimeDesc(user);
    }

    public Bid getHighestBid(Auction auction) {
        return bidRepository.findTopByAuctionOrderByAmountDesc(auction)
            .orElse(null);
    }
} 