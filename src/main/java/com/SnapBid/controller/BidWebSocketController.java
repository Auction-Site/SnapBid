package com.SnapBid.controller;

import com.SnapBid.model.BidMessage;
import com.SnapBid.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class BidWebSocketController {

    private final BidService bidService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public BidWebSocketController(BidService bidService, SimpMessagingTemplate messagingTemplate) {
        this.bidService = bidService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/bid")
    public void processBid(@Payload BidMessage bidMessage) {
        LocalDateTime now = LocalDateTime.now();
        bidMessage.setTimestamp(now.format(FORMATTER));
        messagingTemplate.convertAndSend(
            "/topic/auction/" + bidMessage.getAuctionId(),
            bidMessage
        );
    }
} 