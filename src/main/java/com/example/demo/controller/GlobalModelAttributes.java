package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute
    public void addUserAttributes(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId"); // ✅ Long matches User.userId

        if (userId != null) {
            // ✅ Always fetch fresh from DB so profile changes reflect immediately
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                model.addAttribute("loggedIn",  true);
                model.addAttribute("userId",    userId);
                // ✅ Use DB values, not potentially stale session values
                model.addAttribute("userName",  user.getName());
                model.addAttribute("userEmail", user.getEmail());
                model.addAttribute("userPhone", user.getPhone() != null ? user.getPhone() : "");
                // ✅ Also expose full user object for templates that need user.userId etc.
                model.addAttribute("user",      user);
                return;
            }
        }

        // Not logged in — set safe defaults
        model.addAttribute("loggedIn",  false);
        model.addAttribute("userId",    null);
        model.addAttribute("userName",  null);
        model.addAttribute("userEmail", null);
        model.addAttribute("userPhone", null);
        model.addAttribute("user",      null);
    }
}