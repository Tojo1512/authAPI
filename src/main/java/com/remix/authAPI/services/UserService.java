package com.remix.authAPI.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.remix.authAPI.entity.User;
import com.remix.authAPI.repositories.UserRepository;

import java.util.List;
import java.security.SecureRandom;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;
    
    @Value("${app.security.max-login-attempts}")
    private Integer maxLoginAttempts;

    @Value("${app.security.two-factor-code-length}")
    private int twoFactorCodeLength;

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

     @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("L'email existe déjà");
        }

        // Encoder le mot de passe
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        
        // Générer le token de vérification
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
        
        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);

        // Envoyer l'email de vérification
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken;
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);

        return savedUser;
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new RuntimeException("Token de vérification invalide"));

        if (user.getIsEmailVerified()) {
            throw new RuntimeException("Cet email a déjà été vérifié");
        }

        if (user.getEmailVerificationExpiry() == null || 
            LocalDateTime.now().isAfter(user.getEmailVerificationExpiry())) {
            
            // Générer un nouveau token
            String newToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(newToken);
            user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            
            // Envoyer un nouveau mail
            String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + newToken;
            emailService.sendVerificationEmail(user.getEmail(), verificationLink);
            
            throw new RuntimeException("Le lien de vérification a expiré. Un nouveau lien vous a été envoyé par email.");
        }

        // Marquer l'email comme vérifié et supprimer le token
        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public User authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier si le compte est verrouillé
        if (isAccountLocked(email)) {
            throw new RuntimeException("Compte temporairement verrouillé. Veuillez réessayer plus tard.");
        }

        // Vérifier si l'email est vérifié
        if (!user.getIsEmailVerified()) {
            throw new RuntimeException("Veuillez vérifier votre email avant de vous connecter");
        }

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            incrementFailedLoginAttempts(email);
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Réinitialiser les tentatives de connexion en cas de succès
        resetFailedLoginAttempts(email);
        
        return user;
    }


    private String generateTwoFactorCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < twoFactorCodeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    @Transactional
    public User initiateLogin(String email, String password) {
        User user = authenticateUser(email, password);
        
        // Générer et sauvegarder le code 2FA
        String twoFactorCode = generateTwoFactorCode();
        user.setTwoFactorCode(twoFactorCode);
        user.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        
        // Envoyer le code par email
        emailService.send2FACode(user.getEmail(), twoFactorCode);
        
        return user;
    }

    @Transactional
    public User verifyTwoFactorCode(String email, String code) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (isAccountLocked(email)) {
            throw new RuntimeException("Compte temporairement verrouillé. Vérifiez vos emails pour le débloquer.");
        }

        if (user.getTwoFactorCode() == null || user.getTwoFactorCodeExpiry() == null) {
            throw new RuntimeException("Aucun code 2FA n'a été généré");
        }

        if (LocalDateTime.now().isAfter(user.getTwoFactorCodeExpiry())) {
            throw new RuntimeException("Le code 2FA a expiré");
        }

        if (!user.getTwoFactorCode().equals(code)) {
            increment2FAAttempts(email);
            throw new RuntimeException("Code 2FA invalide");
        }

        // Réinitialiser les tentatives en cas de succès
        reset2FAAttempts(email);
        resetFailedLoginAttempts(email);

        // Nettoyer le code 2FA après utilisation
        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiry(null);
        return userRepository.save(user);
    }

    @Transactional
    public void increment2FAAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailed2FAAttempts(user.getFailed2FAAttempts() + 1);
            user.setLastFailed2FA(LocalDateTime.now());
            
            if (user.getFailed2FAAttempts() >= maxLoginAttempts) {
                user.setAccountLocked(true);
                user.setAccountLockedUntil(LocalDateTime.now().plusHours(1));
                // Envoyer un email de réinitialisation
                sendUnlockEmail(user.getEmail());
            }
            
            userRepository.save(user);
        });
    }

    @Transactional
    public void reset2FAAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailed2FAAttempts(0);
            user.setLastFailed2FA(null);
            userRepository.save(user);
        });
    }

    private void sendUnlockEmail(String email) {
        String unlockToken = UUID.randomUUID().toString();
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setUnlockToken(unlockToken);
        userRepository.save(user);
        
        String unlockLink = "http://localhost:8080/api/auth/unlock-account?token=" + unlockToken;
        emailService.sendUnlockEmail(email, unlockLink);
    }

    @Transactional
    public void unlockAccount(String token) {
        User user = userRepository.findByUnlockToken(token)
            .orElseThrow(() -> new RuntimeException("Token invalide"));
        
        user.setAccountLocked(false);
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setFailed2FAAttempts(0);
        user.setUnlockToken(null);
        userRepository.save(user);
    }
} 