package com.questionary.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;

        String title;
        String message;
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            title = "403 — Access Denied";
            message = "You don't have permission to perform this action. "
                    + "If your session expired, try refreshing the page and signing in again.";
        } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
            title = "404 — Page Not Found";
            message = "The page you were looking for doesn't exist.";
        } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            title = "401 — Unauthorized";
            message = "Please sign in to access this page.";
        } else {
            title = statusCode + " — Unexpected Error";
            Object msg = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
            message = (msg != null && !msg.toString().isBlank())
                    ? msg.toString()
                    : "An unexpected error occurred. Please try again.";
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        return "error";
    }
}
