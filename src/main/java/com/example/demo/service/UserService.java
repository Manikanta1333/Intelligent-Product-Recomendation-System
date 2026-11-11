package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User register(String name, String email, String password, String phone) {
        if (repo.existsByEmail(email)) {
            return null;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);
        return repo.save(user);
    }

    public User login(String email, String password) {
        Optional<User> opt = repo.findByEmail(email);
        if (opt.isEmpty()) return null;
        User user = opt.get();
        if (user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // ✅ Long — matches UserRepository<User, Long> and User.userId type
    public User findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public User findByEmail(String email) {
        return repo.findByEmail(email).orElse(null);
    }

    public User save(User user) {
        return repo.save(user);
    }

    public boolean emailExists(String email) {
        return repo.existsByEmail(email);
    }
}