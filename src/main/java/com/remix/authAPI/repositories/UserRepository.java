package com.remix.authAPI.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.remix.authAPI.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailVerificationToken(String token);
} 