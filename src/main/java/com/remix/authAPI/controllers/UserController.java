package com.remix.authAPI.controllers;

import com.remix.authAPI.models.User;
import com.remix.authAPI.services.UserService;
import com.remix.authAPI.response.ResponseHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController extends ResponseHandler {

    private final UserService userService;

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
        } catch (Exception e) {
            return generateErrorResponse("Erreur lors de la création de l'utilisateur", HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.findById(id)
                .map(existingUser -> {
                    user.setId(id);
                    User updatedUser = userService.updateUser(user);
                    return generateSuccessResponse(updatedUser);
                })
                .orElse(generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
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
} 