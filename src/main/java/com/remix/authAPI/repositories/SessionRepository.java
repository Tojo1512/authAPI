package com.remix.authAPI.repositories;

import com.remix.authAPI.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
    
    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < ?1 OR s.lastActivity < ?2")
    void deleteExpiredSessions(LocalDateTime expirationTime, LocalDateTime inactivityTime);
    
    void deleteByUser_Id(Long userId);
} 