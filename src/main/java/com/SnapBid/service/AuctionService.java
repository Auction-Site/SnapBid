package com.SnapBid.service;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.repository.AuctionRepository;
import com.SnapBid.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionService(BidRepository bidRepository) {
        this.bidRepository = bidRepository;
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    public Optional<Auction> getAuctionById(Long id) {
        return auctionRepository.findById(id);
    }

    public Auction createAuction(Auction auction) {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCreatedAt(LocalDateTime.now());
        auction.setUpdatedAt(LocalDateTime.now());
        return auctionRepository.save(auction);
    }

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

    public void deleteAuction(Long id) {
        auctionRepository.deleteById(id);
    }

    public List<Auction> getActiveAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE);
    }

    public List<Auction> getEndedAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ENDED);
    }

    public void endAuction(Long id) {
        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isPresent()) {
            Auction auction = optionalAuction.get();
            auction.setStatus(AuctionStatus.ENDED);
            auction.setUpdatedAt(LocalDateTime.now());
            auctionRepository.save(auction);
        }
    }

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
    public Auction createAuction(Auction auction, User seller) {
        auction.setSeller(seller);
        auction.setCurrentPrice(auction.getStartingPrice());
        return auctionRepository.save(auction);
    }

    @Transactional
    public Bid placeBid(Auction auction, User bidder, BigDecimal amount) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new RuntimeException("Auction is not active");
        }

        if (auction.getEndDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Auction has ended");
        }

        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new RuntimeException("Bid amount must be higher than current price");
        }

        if (auction.getSeller().equals(bidder)) {
            throw new RuntimeException("You cannot bid on your own auction");
        }

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);

        auction.setCurrentPrice(amount);
        auctionRepository.save(auction);

        return bidRepository.save(bid);
    }

    public List<Auction> getUserAuctions(User user) {
        return auctionRepository.findBySeller(user);
    }

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
        return auctionRepository.searchByKeyword(keyword);
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
} 