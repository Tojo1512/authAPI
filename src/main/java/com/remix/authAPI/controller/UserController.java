package com.remix.authAPI.controller;

import com.remix.authAPI.entity.User;
import com.remix.authAPI.service.UserService;
import com.remix.authAPI.response.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
public class UserController extends ResponseHandler {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        List<User> users = userService.findAll();
        return generateSuccessResponse(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(this::generateSuccessResponse)
                .orElse(generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody User user) {
        try {
            User savedUser = userService.save(user);
            return generateSuccessResponse(savedUser);
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return generateErrorResponse("Erreur lors de la création: " + e.getMessage(), 
                                      HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            return userService.findById(id)
                .map(existingUser -> {
                    // Mise à jour des champs modifiables
                    user.setId(id);
                    
                    // Ne pas permettre la modification directe du password_hash
                    user.setPasswordHash(existingUser.getPasswordHash());
                    
                    // Conserver la date de création
                    user.setCreatedAt(existingUser.getCreatedAt());
                    
                    // Mettre à jour la date de modification
                    user.setUpdatedAt(LocalDateTime.now());
                    
                    // Valider l'email
                    if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
                        if (userService.findByEmail(user.getEmail()).isPresent()) {
                            return generateErrorResponse("Cet email est déjà utilisé", HttpStatus.BAD_REQUEST);
                        }
                    }

                    User updatedUser = userService.updateUser(user);
                    return generateSuccessResponse(updatedUser);
                })
                .orElse(generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return generateErrorResponse("Erreur lors de la mise à jour: " + e.getMessage(), 
                                      HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        if (userService.findById(id).isPresent()) {
            userService.deleteById(id);
            return generateSuccessResponse("Utilisateur supprimé avec succès");
        }
        return generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/check-login-attempts/{email}")
    public ResponseEntity<Object> checkLoginAttempts(@PathVariable String email) {
        boolean isExceeded = userService.isLoginAttemptsExceeded(email);
        return generateSuccessResponse(Map.of(
            "isExceeded", isExceeded,
            "email", email
        ));
    }

    @PostMapping("/login/initiate")
    public ResponseEntity<Object> initiateLogin(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        
        return userService.initiateLogin(email, password)
            .map(user -> generateSuccessResponse("Code PIN envoyé par email"))
            .orElse(generateErrorResponse("Identifiants invalides", HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/login/verify")
    public ResponseEntity<Object> verifyPinAndLogin(@RequestBody Map<String, String> verificationData) {
        String email = verificationData.get("email");
        String pin = verificationData.get("pin");
        
        return userService.verifyPinAndLogin(email, pin)
            .map(user -> generateSuccessResponse("Connexion réussie"))
            .orElse(generateErrorResponse("Code PIN invalide ou expiré", HttpStatus.UNAUTHORIZED));
    }


    
} 