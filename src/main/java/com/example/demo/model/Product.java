package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    private Integer discount;

    private String name;

    @Column(name = "main_category")
    private String mainCategory;

    private String image;

    private Double ratings;

    @Column(name = "no_of_ratings")
    private Integer noOfRatings;

    @Column(name = "actual_price")
    private Double actualPrice;

    private String brand;

    // ===================================================
    // HELPER METHODS
    // ===================================================

    public String getFormattedPrice() {
        if (actualPrice == null) return "₹0";
        return String.format("₹%,.0f", actualPrice);
    }

    public Double getSalePrice() {
        if (actualPrice == null) return 0.0;
        if (discount == null || discount <= 0) return actualPrice;
        return Math.round((actualPrice - (actualPrice * discount / 100.0)) * 100.0) / 100.0;
    }

    public String getFormattedSalePrice() {
        return String.format("₹%,.0f", getSalePrice());
    }

    /** Called by Thymeleaf as p.hasImage() */
    public boolean hasImage() {
        return image != null && !image.trim().isEmpty();
    }

    /** Bean-style alias — kept for any code using isHasImage() */
    public boolean isHasImage() {
        return hasImage();
    }

    /**
     * Maps ALL 27 real DB categories to emoji icons
     */
    public String getCategoryIcon() {
        if (mainCategory == null) return "🛒";
        switch (mainCategory.trim()) {
            case "Baby Products":           return "🍼";
            case "Cameras":                 return "📷";
            case "Handbags and Clutches":   return "👜";
            case "Headphones":              return "🎧";
            case "Home Improvement":        return "🔧";
            case "Kids Clothing":           return "👶";
            case "Make-up":                 return "💄";
            case "Mens Formal Shoes":       return "👞";
            case "Mens Innerwear":          return "🩲";
            case "Mens Shirts":             return "👔";
            case "Mens Sports Shoes":       return "👟";
            case "Mens Watches":            return "⌚";
            case "Motorbike Accessories":   return "🏍️";
            case "Musical Instruments":     return "🎸";
            case "Phones":                  return "📱";
            case "Refrigerators":           return "🧊";
            case "School Bags":             return "🎒";
            case "Speakers":                return "🔊";
            case "Strength Training":       return "🏋️";
            case "T-shirts and Polos":      return "👕";
            case "Televisions":             return "📺";
            case "Toys":                    return "🧸";
            case "Womens Fashion":          return "👗";
            case "Womens Innerwear":        return "👙";
            case "Womens Sandals":          return "👡";
            case "Womens Shoes":            return "👠";
            case "Womens Watches":          return "⌚";
            default:                        return "🛒";
        }
    }

    public String getStarDisplay() {
        if (ratings == null) return "☆☆☆☆☆";
        int full  = (int) Math.floor(ratings);
        int empty = 5 - full;
        return "★".repeat(Math.min(full, 5)) + "☆".repeat(Math.max(empty, 0));
    }

    // ===================================================
    // GETTERS & SETTERS
    // ===================================================

    public Integer getProductId()  { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getDiscount()   { return discount; }
    public void setDiscount(Integer discount) { this.discount = discount; }

    public String getName()        { return name; }
    public void setName(String name) { this.name = name; }

    public String getMainCategory() { return mainCategory; }
    public void setMainCategory(String mainCategory) { this.mainCategory = mainCategory; }

    public String getImage()       { return image; }
    public void setImage(String image) { this.image = image; }

    public Double getRatings()     { return ratings; }
    public void setRatings(Double ratings) { this.ratings = ratings; }

    public Integer getNoOfRatings() { return noOfRatings; }
    public void setNoOfRatings(Integer noOfRatings) { this.noOfRatings = noOfRatings; }

    public Double getActualPrice() { return actualPrice; }
    public void setActualPrice(Double actualPrice) { this.actualPrice = actualPrice; }

    public String getBrand()       { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}