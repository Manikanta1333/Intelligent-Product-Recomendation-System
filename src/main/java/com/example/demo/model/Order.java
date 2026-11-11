package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")   // ← PK is order_id
    private Integer orderId;

    @Column(name = "user_id")    // ← FK references users.user_id
    private Integer userId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_image", columnDefinition = "TEXT")
    private String productImage;

    @Column(name = "brand")
    private String brand;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "price")
    private Double price;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status")
    private String status = "Processing";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();

    // ===== CONSTRUCTORS =====
    public Order() {}

    // ===== HELPERS =====
    public String getFormattedDate() {
        if (orderDate == null) return "";
        return orderDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String getFormattedTotal() {
        if (totalAmount == null) return "₹0";
        return String.format("₹%,.0f", totalAmount);
    }

    public String getFormattedPrice() {
        if (price == null) return "₹0";
        return String.format("₹%,.0f", price);
    }

    // e.g. MD000042
    public String getOrderCode() {
        return "MD" + String.format("%06d", orderId != null ? orderId : 0);
    }

    public String getStatusClass() {
        if (status == null) return "status-processing";
        switch (status.toLowerCase()) {
            case "delivered":  return "status-delivered";
            case "shipped":    return "status-shipped";
            case "cancelled":  return "status-cancelled";
            default:           return "status-processing";
        }
    }

    public String getStatusEmoji() {
        if (status == null) return "⏳";
        switch (status.toLowerCase()) {
            case "delivered":  return "✅";
            case "shipped":    return "🚚";
            case "cancelled":  return "❌";
            default:           return "⏳";
        }
    }

    // ===== GETTERS & SETTERS =====
    // getId() kept for backward compat (OrderService uses .save(), OrderRepository uses id)
    public Integer getId()           { return orderId; }
    public void    setId(Integer id) { this.orderId = id; }

    public Integer getOrderId()                { return orderId; }
    public void    setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getUserId()               { return userId; }
    public void    setUserId(Integer userId) { this.userId = userId; }

    public Integer getProductId()                  { return productId; }
    public void    setProductId(Integer productId) { this.productId = productId; }

    public String  getProductName()                    { return productName; }
    public void    setProductName(String productName)  { this.productName = productName; }

    public String  getProductImage()                     { return productImage; }
    public void    setProductImage(String productImage)  { this.productImage = productImage; }

    public String  getBrand()              { return brand; }
    public void    setBrand(String brand)  { this.brand = brand; }

    public Integer getQuantity()                 { return quantity; }
    public void    setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double  getPrice()              { return price; }
    public void    setPrice(Double price)  { this.price = price; }

    public Double  getTotalAmount()                    { return totalAmount; }
    public void    setTotalAmount(Double totalAmount)  { this.totalAmount = totalAmount; }

    public String  getStatus()               { return status; }
    public void    setStatus(String status)  { this.status = status; }

    public String  getPaymentMethod()                      { return paymentMethod; }
    public void    setPaymentMethod(String paymentMethod)  { this.paymentMethod = paymentMethod; }

    public String  getShippingAddress()                        { return shippingAddress; }
    public void    setShippingAddress(String shippingAddress)  { this.shippingAddress = shippingAddress; }

    public LocalDateTime getOrderDate()                    { return orderDate; }
    public void          setOrderDate(LocalDateTime date)  { this.orderDate = date; }
}