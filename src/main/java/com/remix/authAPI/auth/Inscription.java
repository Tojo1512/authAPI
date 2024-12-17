package com.remix.authAPI.auth;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.remix.authAPI.response.ResponseHandler;
import com.remix.authAPI.services.EmailService;
import com.remix.authAPI.models.User;
import com.remix.authAPI.repositories.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class Inscription {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User user) {
        System.out.println("Début de l'inscription pour : " + user.getEmail());
        
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            System.out.println("Email déjà existant : " + user.getEmail());
            return ResponseHandler.generateResponse("L'email existe déjà", false, 400, null);
        }

        try {
            // Encoder le mot de passe
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            
            // Générer le token de vérification
            String verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(verificationToken);
            user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
            
            // Sauvegarder l'utilisateur
            User savedUser = userRepository.save(user);
            System.out.println("Utilisateur sauvegardé avec l'ID : " + savedUser.getId());

            // Envoyer l'email de vérification
            String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken;
            emailService.sendVerificationEmail(user.getEmail(), verificationLink);
            System.out.println("Email de vérification envoyé à : " + user.getEmail());

            return ResponseHandler.generateResponse(
                "Inscription réussie. Veuillez vérifier votre email", 
                true, 
                201, 
                savedUser
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'inscription : " + e.getMessage());
            return ResponseHandler.generateResponse(
                "Erreur lors de l'inscription", 
                false, 
                500, 
                null
            );
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Object> test() {
        return ResponseHandler.generateResponse(
            "L'API fonctionne correctement", 
            true, 
            200, 
            null
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(@RequestParam String token) {
        System.out.println("Tentative de vérification avec le token : " + token);
        
        Optional<User> userOptional = userRepository.findByEmailVerificationToken(token);
        
        if (userOptional.isEmpty()) {
            System.out.println("Token non trouvé : " + token);
            return ResponseHandler.generateResponse(
                "Token de vérification invalide", 
                false, 
                400, 
                null
            );
        }
        
        User user = userOptional.get();
        
        // Vérifier si le token n'a pas expiré
        if (user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            System.out.println("Token expiré pour l'utilisateur : " + user.getEmail());
            return ResponseHandler.generateResponse(
                "Le lien de vérification a expiré", 
                false, 
                400, 
                null
            );
        }
        
        // Marquer l'email comme vérifié
        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);
        
        System.out.println("Email vérifié avec succès pour : " + user.getEmail());
        
        return ResponseHandler.generateResponse(
            "Email vérifié avec succès", 
            true, 
            200, 
            null
        );
    }
}
