package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final OrderRepository orderRepo;

    public ProductService(ProductRepository repo, OrderRepository orderRepo) {
        this.repo = repo;
        this.orderRepo = orderRepo;
    }

    public List<Product> getAllProducts() {
        return repo.findAll();
    }

    public Product getProductById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public List<Product> getFeaturedProducts() {
        return repo.findTop8ByOrderByRatingsDesc();
    }

    public List<Product> filterProducts(String category, String search) {
        String cat   = (category != null && !category.isBlank()) ? category : null;
        String query = (search   != null && !search.isBlank())   ? search   : null;
        if (cat == null && query == null) return repo.findAll();
        return repo.findByCategoryAndSearch(cat, query);
    }

    public List<String> getAllCategories() {
        return repo.findAllCategories();
    }

    public List<Product> getTopByCategory(String category, int limit) {
        return repo.findByMainCategory(category)
                   .stream()
                   .sorted(Comparator.comparingDouble(
                       p -> p.getRatings() != null ? -p.getRatings() : 0))
                   .limit(limit)
                   .collect(Collectors.toList());
    }

    // ✅ Takes Integer userId — matches OrderRepository which uses Integer
    public List<Product> getRecommendationsForUser(Integer userId) {
        List<Order> orders = orderRepo.findByUserIdOrderByOrderDateDesc(userId);

        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> orderedProductIds = orders.stream()
                .map(Order::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Long> categoryFrequency = orders.stream()
                .map(order -> {
                    if (order.getProductId() != null) {
                        Product p = repo.findById(order.getProductId()).orElse(null);
                        return p != null ? p.getMainCategory() : null;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        List<String> preferredCategories = categoryFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Product> recommendations = new ArrayList<>();
        for (String category : preferredCategories) {
            List<Product> categoryProducts = repo.findByMainCategory(category)
                    .stream()
                    .filter(p -> !orderedProductIds.contains(p.getProductId()))
                    .filter(p -> p.getRatings() != null && p.getRatings() >= 3.5)
                    .sorted(Comparator.comparingDouble(p -> -p.getRatings()))
                    .limit(4)
                    .collect(Collectors.toList());
            recommendations.addAll(categoryProducts);
            if (recommendations.size() >= 16) break;
        }

        if (recommendations.size() < 8) {
            Set<Integer> alreadyAdded = recommendations.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toSet());
            alreadyAdded.addAll(orderedProductIds);

            List<Product> filler = repo.findTop8ByOrderByRatingsDesc()
                    .stream()
                    .filter(p -> !alreadyAdded.contains(p.getProductId()))
                    .collect(Collectors.toList());
            recommendations.addAll(filler);
        }

        return recommendations.stream().limit(16).collect(Collectors.toList());
    }
}