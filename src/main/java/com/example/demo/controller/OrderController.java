package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;

    public OrderController(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer qty,
            HttpSession session,
            Model model) {

        // ✅ Session stores Long (from User.getUserId())
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        if (productId != null) {
            Product product = productService.getProductById(productId);
            if (product != null) {
                model.addAttribute("buyNowProduct", product);
                model.addAttribute("buyNowQty", qty != null ? qty : 1);
            }
        }

        model.addAttribute("userName", session.getAttribute("userName"));
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String productImage,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) String state,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // ✅ Cast Long from session, then convert to Integer for OrderService
        Long userIdLong = (Long) session.getAttribute("userId");
        if (userIdLong == null) return "redirect:/login";
        Integer userId = userIdLong.intValue(); // ✅ Safe conversion — Order uses Integer userId

        String shippingAddress = String.format("%s %s, %s, %s - %s, %s",
                firstName != null ? firstName : "",
                lastName  != null ? lastName  : "",
                address   != null ? address   : "",
                city      != null ? city      : "",
                pincode   != null ? pincode   : "",
                state     != null ? state     : "");

        String payment = paymentMethod != null ? paymentMethod : "COD";

        if (productId != null && price != null) {
            Order order = orderService.placeOrder(
                    userId, productId, productName, productImage,
                    brand, quantity, price, payment, shippingAddress);

            redirectAttributes.addFlashAttribute("orderSuccess", true);
            redirectAttributes.addFlashAttribute("orderId", order.getOrderCode());
        } else {
            redirectAttributes.addFlashAttribute("orderSuccess", true);
            redirectAttributes.addFlashAttribute("orderId", "MD" + System.currentTimeMillis() % 1000000);
        }

        return "redirect:/order-success";
    }

    @GetMapping("/order-success")
    public String orderSuccess(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";
        return "order-success";
    }

    @GetMapping("/orders")
    public String ordersPage(HttpSession session, Model model) {
        // ✅ Get Long from session, convert to Integer for OrderService
        Long userIdLong = (Long) session.getAttribute("userId");
        if (userIdLong == null) return "redirect:/login";
        Integer userId = userIdLong.intValue();

        List<Order> orders = orderService.getOrdersByUserId(userId);
        model.addAttribute("orders", orders);
        model.addAttribute("userName", session.getAttribute("userName"));
        return "orders";
    }
}