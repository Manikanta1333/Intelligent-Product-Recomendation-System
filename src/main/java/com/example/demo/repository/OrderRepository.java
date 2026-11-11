package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Finds all orders for a user, newest first
    // userId matches the Java field name in Order.java
    List<Order> findByUserIdOrderByOrderDateDesc(Integer userId);
}