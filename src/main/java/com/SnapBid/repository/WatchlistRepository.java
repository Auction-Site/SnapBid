package com.SnapBid.repository;

import com.SnapBid.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUser_Id(Long userId);
    boolean existsByUser_IdAndAuction_Id(Long userId, Long auctionId);
    void deleteByUser_IdAndAuction_Id(Long userId, Long auctionId);
} 