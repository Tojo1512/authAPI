package com.remix.authAPI.service;

import com.remix.authAPI.entity.User;
import com.remix.authAPI.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.security.max-login-attempts}")
    private Integer maxLoginAttempts;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
        // Vérifier les champs requis
        if (user.getEmail() == null) {
            throw new IllegalArgumentException("Email requis");
        }
        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Password hash requis");
        }

        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Crypter le mot de passe avant de le sauvegarder
        String hashedPassword = BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);

        // Définir les valeurs par défaut si non fournies
        if (user.getIsEmailVerified() == null) user.setIsEmailVerified(false);
        if (user.getFailedLoginAttempts() == null) user.setFailedLoginAttempts(0);
        if (user.getAccountLocked() == null) user.setAccountLocked(false);
        if (user.getStatus() == null) user.setStatus("déconnecté");

        // Définir les timestamps si non fournis
        LocalDateTime now = LocalDateTime.now();
        if (user.getCreatedAt() == null) user.setCreatedAt(now);
        if (user.getUpdatedAt() == null) user.setUpdatedAt(now);

        // Sauvegarder avec JPA
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
        // Vérifier si l'utilisateur existe
        User existingUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Mise à jour des champs
        user.setUpdatedAt(LocalDateTime.now());
        
        // Si vous voulez changer le mot de passe, utilisez la méthode dédiée
        // Ne pas permettre la modification directe du password_hash
        user.setPasswordHash(existingUser.getPasswordHash());

        return userRepository.save(user);
    }
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    public boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
    public void registerUser(String email, String password) {
        String hashedPassword = hashPassword(password); // Hachage du mot de passe
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hashedPassword);  // Enregistrer le mot de passe haché
        userRepository.save(user);
    }
    
    @Transactional
    public Optional<User> login(String email, String password) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT * FROM verify_login(?, ?)",
                new BeanPropertyRowMapper<>(User.class),
                email,
                password
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public User saveComplete(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> initiateLogin(String email, String password) {
        try {
            // Vérifier les credentials
            Optional<User> userOpt = findByEmail(email)
                .filter(u -> BCrypt.checkpw(password, u.getPasswordHash()));
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Générer et sauvegarder le PIN
                String pin = generatePin();
                savePin(user.getId(), pin);
                // Envoyer le PIN par email
                sendPinByEmail(user.getEmail(), pin);
                return userOpt;
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String generatePin() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void savePin(Long userId, String pin) {
        jdbcTemplate.update(
            "INSERT INTO mfa_tokens (user_id, token, expires_at) VALUES (?, ?, ?)",
            userId, pin, LocalDateTime.now().plusMinutes(5)
        );
    }

    private void sendPinByEmail(String email, String pin) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Code de vérification");
        message.setText("Votre code de vérification est : " + pin + "\nCe code expire dans 5 minutes.");
        mailSender.send(message);
    }

    @Transactional
    public Optional<User> verifyPinAndLogin(String email, String pin) {
        return findByEmail(email)
            .filter(user -> verifyPin(user.getId(), pin))
            .map(user -> {
                user.setStatus("connecté");
                return userRepository.save(user);
            });
    }

    private boolean verifyPin(Long userId, String pin) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM mfa_tokens " +
                "WHERE user_id = ? AND token = ? AND used = false " +
                "AND expires_at > NOW())",
                Boolean.class,
                userId, pin
            );
        } catch (Exception e) {
            return false;
        }
    }

} 