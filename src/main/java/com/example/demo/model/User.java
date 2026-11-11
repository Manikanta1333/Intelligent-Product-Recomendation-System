package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "phone")
    private String phone;

    // ── Constructors ──────────────────────────────────────────────────
    public User() {}

    public User(String name, String email, String password) {
        this.name     = name;
        this.email    = email;
        this.password = password;
    }

    // ── Getters & Setters ─────────────────────────────────────────────
    public Long   getUserId()  { return userId; }
    public String getName()    { return name; }
    public String getEmail()   { return email; }
    public String getPassword(){ return password; }
    public String getPhone()   { return phone; }

    public void setUserId(Long userId)    { this.userId   = userId; }
    public void setName(String name)      { this.name     = name; }
    public void setEmail(String email)    { this.email    = email; }
    public void setPassword(String pwd)   { this.password = pwd; }
    public void setPhone(String phone)    { this.phone    = phone; }
}