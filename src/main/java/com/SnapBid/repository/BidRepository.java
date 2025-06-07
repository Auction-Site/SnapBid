package com.SnapBid.repository;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuction(Auction auction);
    List<Bid> findByBidder(User bidder);
    List<Bid> findByAuctionAndBidder(Auction auction, User bidder);
    
    List<Bid> findByAuction_IdOrderByAmountDesc(Long auctionId);
    List<Bid> findByBidder_Id(Long userId);
    
    @Query("SELECT b FROM Bid b WHERE b.auction = :auction ORDER BY b.amount DESC")
    List<Bid> findByAuctionOrderByAmountDesc(@Param("auction") Auction auction);
    
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC")
    List<Bid> findHighestBidsForAuction(@Param("auctionId") Long auctionId);

    List<Bid> findByAuctionOrderByBidTimeDesc(Auction auction);
    List<Bid> findByBidderOrderByBidTimeDesc(User bidder);
    Optional<Bid> findTopByAuctionOrderByAmountDesc(Auction auction);

    // Method to find bids by bidder and auction status (for 'My Bids' or 'Won Auctions' logic)
    List<Bid> findByBidderAndAuctionStatus(User bidder, AuctionStatus status);
} 