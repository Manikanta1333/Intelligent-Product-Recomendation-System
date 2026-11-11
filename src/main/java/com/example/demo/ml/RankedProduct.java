package com.example.demo.ml;

import com.example.demo.model.Product;

/**
 * Wraps a Product with its collaborative, content-based, and hybrid scores.
 * Delegate getters expose Product fields directly so Thymeleaf templates
 * can call rp.image, rp.brand, rp.productName, etc. without going through rp.product.
 */
public class RankedProduct {

    private final Product product;
    private final double  collaborativeScore;
    private final double  contentScore;
    private double        hybridScore;

    public RankedProduct(Product product,
                         double collaborativeScore,
                         double contentScore,
                         double hybridScore) {
        this.product            = product;
        this.collaborativeScore = collaborativeScore;
        this.contentScore       = contentScore;
        this.hybridScore        = hybridScore;
    }

    /** Multiply the hybrid score by a modifier (e.g. intent boosts). */
    public void applyModifier(double modifier) {
        this.hybridScore *= modifier;
    }

    // ── Core fields ──────────────────────────────────────────────
    public Product getProduct()            { return product; }
    public double  getCollaborativeScore() { return collaborativeScore; }
    public double  getContentScore()       { return contentScore; }
    public double  getHybridScore()        { return hybridScore; }

    // ── Delegate getters (used directly in Thymeleaf templates) ──

    public Integer getProductId() {
        return product != null ? product.getProductId() : null;
    }

    public String getProductName() {
        return product != null ? product.getName() : "";
    }

    public String getBrand() {
        return product != null ? product.getBrand() : "";
    }

    public String getImage() {
        return product != null ? product.getImage() : null;
    }

    public boolean hasImage() {
        return product != null && product.getImage() != null && !product.getImage().trim().isEmpty();
    }

    public String getMainCategory() {
        return product != null ? product.getMainCategory() : "";
    }

    public String getCategoryIcon() {
        return product != null ? product.getCategoryIcon() : "🛒";
    }

    public Double getRatings() {
        return product != null ? product.getRatings() : null;
    }

    public String getStarDisplay() {
        return product != null ? product.getStarDisplay() : "☆☆☆☆☆";
    }

    public Integer getDiscount() {
        return product != null ? product.getDiscount() : null;
    }

    public Double getActualPrice() {
        return product != null ? product.getActualPrice() : null;
    }

    public Double getSalePrice() {
        return product != null ? product.getSalePrice() : null;
    }

    public String getFormattedPrice() {
        return product != null ? product.getFormattedPrice() : "₹0";
    }

    public String getFormattedSalePrice() {
        return product != null ? product.getFormattedSalePrice() : "₹0";
    }
}