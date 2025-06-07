package com.SnapBid.controller;

import com.SnapBid.model.Auction;
import com.SnapBid.model.Bid;
import com.SnapBid.model.User;
import com.SnapBid.service.AuctionService;
import com.SnapBid.service.BidService;
import com.SnapBid.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public ProfileController(UserService userService, AuctionService auctionService, BidService bidService) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String profile(Model model) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If not authenticated, redirect to login
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login?error=notAuthenticated";
        }
        
        try {
            User user = userService.findByUsername(auth.getName());
            
            // Get user's auctions
            List<Auction> userAuctions = auctionService.getUserAuctions(user);
            
            // Get user's bids
            List<Auction> biddedAuctions = auctionService.getUserBids(user);
            
            // Get user's won auctions
            List<Auction> wonAuctions = auctionService.getUserWonAuctions(user);
            
            // Add to model
            model.addAttribute("user", user);
            model.addAttribute("userAuctions", userAuctions);
            model.addAttribute("biddedAuctions", biddedAuctions);
            model.addAttribute("wonAuctions", wonAuctions);
            model.addAttribute("title", "Profile");
            
            return "profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/login?error=profile";
        }
    }

    @GetMapping("/edit")
    @Transactional(readOnly = true)
    public String showEditProfileForm(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(currentUser.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("title", "Edit Profile");
        return "user/settings";
    }

    @PostMapping("/update-details")
    @Transactional
    public String updateUserDetails(@Valid @ModelAttribute("user") User userDetails,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal User currentUser,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        User existingUser = userService.findByUsername(currentUser.getUsername());
        if (existingUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/profile";
        }

        // Validate basic user details (excluding password fields for now)
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userDetails); // Keep user input
            model.addAttribute("title", "Edit Profile");
            return "user/settings";
        }

        try {
            // Update only the fields that are allowed to be updated through this form
            // existingUser.setFirstName(userDetails.getFirstName());
            // existingUser.setLastName(userDetails.getLastName());
            existingUser.setEmail(userDetails.getEmail());
            // existingUser.setPhoneNumber(userDetails.getPhoneNumber());
            // existingUser.setAddress(userDetails.getAddress());

            userService.updateUser(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/profile";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }

    @PostMapping("/change-password")
    @Transactional
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirm password do not match.");
            return "redirect:/profile/edit";
        }

        try {
            userService.changeUserPassword(currentUser.getUsername(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error changing password: " + e.getMessage());
            return "redirect:/profile/edit";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while changing password.");
            return "redirect:/profile/edit";
        }
    }
} 