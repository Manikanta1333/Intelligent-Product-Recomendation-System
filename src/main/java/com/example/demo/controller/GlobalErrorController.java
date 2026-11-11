package com.example.demo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GlobalErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status    = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message   = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage",
            message != null && !message.toString().isEmpty()
                ? message.toString()
                : getDefaultMessage(statusCode));

        if (exception != null) {
            System.err.println("ERROR " + statusCode + ": " + exception.toString());
        }

        if (statusCode == 404) {
            return "404";
        }
        return "error";
    }

    private String getDefaultMessage(int code) {
        switch (code) {
            case 403: return "You don't have permission to access this page.";
            case 404: return "The page you're looking for doesn't exist.";
            case 500: return "Something went wrong on our end. Please try again.";
            default:  return "An unexpected error occurred.";
        }
    }
}