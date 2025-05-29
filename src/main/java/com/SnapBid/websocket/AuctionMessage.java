package com.SnapBid.websocket;

import com.SnapBid.model.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class AuctionMessage {
    private Long id;
    private String title;
    private String description;
    private BigDecimal currentPrice;
    private LocalDateTime endDate;
    private String seller;
    private LocalDateTime createdAt;

    public AuctionMessage(Auction auction) {
        this.id = auction.getId();
        this.title = auction.getTitle();
        this.description = auction.getDescription();
        this.currentPrice = auction.getCurrentPrice();
        this.endDate = auction.getEndDate();
        this.seller = auction.getSeller().getUsername();
        this.createdAt = auction.getCreatedAt();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getSeller() { return seller; }
    public void setSeller(String seller) { this.seller = seller; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 