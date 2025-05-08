package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import com.SnapBid.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/auctions")
public class AuctionController {

    private final AuctionService auctionService;
    private final UserService userService;

    public AuctionController(AuctionService auctionService, UserService userService) {
        this.auctionService = auctionService;
        this.userService = userService;
    }

    @GetMapping
    public String listAuctions(Model model) {
        List<Auction> activeAuctions = auctionService.getActiveAuctions();
        model.addAttribute("auctions", activeAuctions);
        return "auction/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("auction", new Auction());
        return "auction/create";
    }

    @PostMapping("/create")
    public String createAuction(@Valid @ModelAttribute("auction") Auction auction,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            return "auction/create";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User seller = userService.findByUsername(auth.getName());

        try {
            auctionService.createAuction(auction, seller);
            return "redirect:/auctions";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auction/create";
        }
    }

    @GetMapping("/{id}")
    public String viewAuction(@PathVariable Long id, Model model) {
        Auction auction = auctionService.findById(id);
        model.addAttribute("auction", auction);
        model.addAttribute("bid", new Bid());
        return "auction/detail";
    }

    @PostMapping("/{id}/bid")
    public String placeBid(@PathVariable Long id,
                          @Valid @ModelAttribute("bid") Bid bid,
                          BindingResult result,
                          Model model) {
        if (result.hasErrors()) {
            return "auction/detail";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User bidder = userService.findByUsername(auth.getName());
        Auction auction = auctionService.findById(id);

        try {
            auctionService.placeBid(auction, bidder, bid.getAmount());
            return "redirect:/auctions/" + id;
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("auction", auction);
            return "auction/detail";
        }
    }

    @GetMapping("/my-auctions")
    public String myAuctions(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        
        List<Auction> myAuctions = auctionService.getUserAuctions(user);
        model.addAttribute("auctions", myAuctions);
        return "auction/my-auctions";
    }

    @GetMapping("/my-bids")
    public String myBids(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        
        List<Auction> myBids = auctionService.getUserBids(user);
        model.addAttribute("auctions", myBids);
        return "auction/my-bids";
    }
} 