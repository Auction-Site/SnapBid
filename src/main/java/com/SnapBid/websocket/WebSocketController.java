package com.SnapBid.websocket;

import com.SnapBid.model.Auction;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class WebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ... existing bid handling methods ...

    public void notifyNewAuction(Auction auction) {
        try {
            if (auction == null) {
                logger.error("Cannot notify about null auction");
                return;
            }

            logger.info("Notifying about new auction: id={}, title={}, seller={}", 
                auction.getId(), 
                auction.getTitle(),
                auction.getSeller().getUsername());
            
            // Create message with auction details
            AuctionMessage message = new AuctionMessage(auction);
            
            // Send message to all connected clients
            messagingTemplate.convertAndSend("/topic/auctions/new", message);
            
            logger.info("Successfully sent notification for auction {} to all connected clients", auction.getId());
        } catch (Exception e) {
            logger.error("Error sending auction notification for auction {}: {}", 
                auction != null ? auction.getId() : "null", 
                e.getMessage(), 
                e);
            throw new RuntimeException("Failed to send auction notification", e);
        }
    }
} 