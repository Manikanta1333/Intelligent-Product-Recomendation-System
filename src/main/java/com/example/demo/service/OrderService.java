package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repo;

    public OrderService(OrderRepository repo) {
        this.repo = repo;
    }

    public List<Order> getOrdersByUserId(Integer userId) {
        return repo.findByUserIdOrderByOrderDateDesc(userId);
    }

    public Order placeOrder(Integer userId,
                            Integer productId,
                            String  productName,
                            String  productImage,
                            String  brand,
                            Integer quantity,
                            Double  price,
                            String  paymentMethod,
                            String  shippingAddress) {

        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductName(productName);
        order.setProductImage(productImage);
        order.setBrand(brand);
        order.setQuantity(quantity != null ? quantity : 1);
        order.setPrice(price);
        order.setTotalAmount(price * (quantity != null ? quantity : 1));
        order.setStatus("Processing");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setShippingAddress(shippingAddress);
        order.setOrderDate(LocalDateTime.now());

        return repo.save(order);
    }

    public Order save(Order order) {
        return repo.save(order);
    }

    public Order findById(Integer orderId) {
        return repo.findById(orderId).orElse(null);
    }
}