package com.SnapBid.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(
            RequestDispatcher.ERROR_STATUS_CODE
        );
        String errorMsg = "Unknown error occurred";

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            if (statusCode == 404) {
                errorMsg = "Page not found";
            } else if (statusCode == 500) {
                errorMsg = "Internal server error";
            }
            model.addAttribute("statusCode", statusCode);
        }
        model.addAttribute("errorMsg", errorMsg);
        return "error"; // Renders error.html from templates/
    }

    public String getErrorPath() {
        return "/error";
    }
}
