package com.SnapBid.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidMessage {
    private Long auctionId;
    private Double bidAmount;
    private String bidderUsername;
    private String timestamp;
    private Double currentHighestBid;
} 