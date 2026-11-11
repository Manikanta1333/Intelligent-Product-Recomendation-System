package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes ra) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            ra.addFlashAttribute("error", "Email and password are required.");
            return "redirect:/login";
        }

        User user = userService.login(email.trim(), password);

        if (user == null) {
            ra.addFlashAttribute("error", "Invalid email or password. Please try again.");
            return "redirect:/login";
        }

        // ✅ Store all session attributes from real DB user
        session.setAttribute("userId",    user.getUserId());
        session.setAttribute("userName",  user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPhone", user.getPhone() != null ? user.getPhone() : "");
        session.setAttribute("user",      user);
        session.setMaxInactiveInterval(60 * 60);

        ra.addFlashAttribute("success", "Welcome back, " + user.getName() + "!");
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String phone,
            HttpSession session,
            RedirectAttributes ra) {

        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "Name is required.");
            return "redirect:/register";
        }
        if (email == null || !email.contains("@")) {
            ra.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/register";
        }
        if (password == null || password.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/register";
        }
        if (userService.emailExists(email.trim())) {
            ra.addFlashAttribute("error", "This email is already registered. Please login.");
            return "redirect:/register";
        }

        User user = userService.register(name.trim(), email.trim(), password, phone);

        if (user == null) {
            ra.addFlashAttribute("error", "Registration failed. Please try again.");
            return "redirect:/register";
        }

        session.setAttribute("userId",    user.getUserId());
        session.setAttribute("userName",  user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPhone", user.getPhone() != null ? user.getPhone() : "");
        session.setAttribute("user",      user);

        ra.addFlashAttribute("success",
            "Account created! Welcome to MD Store, " + user.getName() + "!");
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("success", "You have been logged out successfully.");
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        // ✅ Always load fresh from DB — never rely on session for profile data
        User user = userService.findById(userId);
        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }

        // ✅ Sync session with latest DB values so GlobalModelAttributes stays current
        session.setAttribute("userName",  user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPhone", user.getPhone() != null ? user.getPhone() : "");
        session.setAttribute("user",      user);

        // ✅ Expose all user fields explicitly to template
        model.addAttribute("user",        user);
        model.addAttribute("userId",      user.getUserId());
        model.addAttribute("userName",    user.getName());
        model.addAttribute("userEmail",   user.getEmail());
        model.addAttribute("userPhone",   user.getPhone() != null ? user.getPhone() : "");
        model.addAttribute("loggedIn",    true);
        return "profile";
    }

    // ===== UPDATE PROFILE =====
    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            HttpSession session,
            RedirectAttributes ra) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        User user = userService.findById(userId);
        if (user == null) return "redirect:/login";

        if (name != null && !name.isBlank()) user.setName(name.trim());
        if (phone != null && !phone.isBlank()) user.setPhone(phone.trim());

        userService.save(user);

        // ✅ Update session with new values
        session.setAttribute("userName",  user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPhone", user.getPhone() != null ? user.getPhone() : "");
        session.setAttribute("user",      user);

        ra.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/profile";
    }
}