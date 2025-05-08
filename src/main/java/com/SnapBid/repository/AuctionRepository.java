package com.SnapBid.repository;

import com.SnapBid.model.Auction;
import com.SnapBid.model.AuctionStatus;
import com.SnapBid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatusAndEndDateAfter(AuctionStatus status, LocalDateTime date);
    List<Auction> findBySeller(User seller);
    List<Auction> findByBids_Bidder(User bidder);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findByCategory_Id(Long categoryId);
    List<Auction> findByEndDateBeforeAndStatus(LocalDateTime endDate, AuctionStatus status);
    List<Auction> findBySeller_Id(Long sellerId);
    List<Auction> findByBids_Bidder_Id(Long userId);
    
    @Query("SELECT a FROM Auction a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Auction> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT a FROM Auction a WHERE a.currentPrice BETWEEN :minPrice AND :maxPrice")
    List<Auction> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
} 