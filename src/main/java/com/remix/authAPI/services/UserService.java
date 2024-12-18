package com.remix.authAPI.services;

import com.remix.authAPI.entity.User;
import com.remix.authAPI.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    
    @Value("${app.security.max-login-attempts}")
    private Integer maxLoginAttempts;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public boolean isLoginAttemptsExceeded(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getFailedLoginAttempts() >= maxLoginAttempts;
        }
        return false;
    }

    @Transactional
    public void incrementFailedLoginAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            user.setLastFailedLogin(LocalDateTime.now());
            
            if (isLoginAttemptsExceeded(email)) {
                user.setAccountLocked(true);
                user.setAccountLockedUntil(LocalDateTime.now().plusHours(1)); // Verrouillage pour 1 heure
            }
            
            userRepository.save(user);
        });
    }

    @Transactional
    public void resetFailedLoginAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLogin(null);
            user.setAccountLocked(false);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public boolean isAccountLocked(String email) {
        return userRepository.findByEmail(email)
            .map(user -> {
                if (user.getAccountLocked() && user.getAccountLockedUntil() != null) {
                    // Si la période de verrouillage est passée, déverrouille le compte
                    if (LocalDateTime.now().isAfter(user.getAccountLockedUntil())) {
                        user.setAccountLocked(false);
                        user.setAccountLockedUntil(null);
                        user.setFailedLoginAttempts(0);
                        userRepository.save(user);
                        return false;
                    }
                    return true;
                }
                return false;
            })
            .orElse(false);
    }

    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
} 