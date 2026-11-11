package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Exact match for category
    List<Product> findByMainCategory(String mainCategory);

    // Case-insensitive search by name or brand
    List<Product> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
            String name, String brand);

    // Filter by category + optional search
    @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.mainCategory = :category) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(p.brand) LIKE LOWER(CONCAT('%',:search,'%')))")
    List<Product> findByCategoryAndSearch(
            @Param("category") String category,
            @Param("search") String search);

    // Top 8 by rating for homepage / fallback
    List<Product> findTop8ByOrderByRatingsDesc();

    // Top N by rating within a specific category
    @Query("SELECT p FROM Product p WHERE p.mainCategory = :category " +
           "AND p.ratings IS NOT NULL ORDER BY p.ratings DESC")
    List<Product> findTopByCategory(@Param("category") String category);

    // All distinct categories
    @Query("SELECT DISTINCT p.mainCategory FROM Product p WHERE p.mainCategory IS NOT NULL ORDER BY p.mainCategory")
    List<String> findAllCategories();

    // Products by list of categories (for recommendation engine)
    @Query("SELECT p FROM Product p WHERE p.mainCategory IN :categories " +
           "AND p.ratings >= :minRating ORDER BY p.ratings DESC")
    List<Product> findByCategoriesAndMinRating(
            @Param("categories") List<String> categories,
            @Param("minRating") Double minRating);
}