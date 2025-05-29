package com.SnapBid.websocket;

import com.SnapBid.model.Auction;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ... existing bid handling methods ...

    public void notifyNewAuction(Auction auction) {
        AuctionMessage message = new AuctionMessage(auction);
        messagingTemplate.convertAndSend("/topic/auctions/new", message);
    }
} 