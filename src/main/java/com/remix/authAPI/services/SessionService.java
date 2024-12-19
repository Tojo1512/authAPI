package com.remix.authAPI.services;

import com.remix.authAPI.entity.Session;
import com.remix.authAPI.entity.User;
import com.remix.authAPI.repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    
    @Value("${app.session.timeout-minutes}")
    private int sessionTimeoutMinutes;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public Session createSession(User user) {
        // Supprimer les sessions existantes de l'utilisateur
        sessionRepository.deleteByUser_Id(user.getId());

        Session session = new Session();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionTimeoutMinutes));
        
        return sessionRepository.save(session);
    }

    @Transactional
    public boolean validateAndUpdateSession(String token) {
        return sessionRepository.findByToken(token)
            .map(session -> {
                LocalDateTime now = LocalDateTime.now();
                
                if (now.isAfter(session.getExpiresAt())) {
                    sessionRepository.delete(session);
                    return false;
                }

                session.setLastActivity(now);
                session.setExpiresAt(now.plusMinutes(sessionTimeoutMinutes));
                sessionRepository.save(session);
                return true;
            })
            .orElse(false);
    }

    @Transactional
    public void invalidateSession(String token) {
        sessionRepository.findByToken(token)
            .ifPresent(sessionRepository::delete);
    }

    @Scheduled(fixedRateString = "${app.session.cleanup-interval-minutes}000")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.deleteExpiredSessions(
            now,
            now.minusMinutes(sessionTimeoutMinutes)
        );
    }
} 