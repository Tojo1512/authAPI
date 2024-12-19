package com.remix.authAPI.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.remix.authAPI.dto.LoginRequest;
import com.remix.authAPI.entity.User;
import com.remix.authAPI.entity.Session;
import com.remix.authAPI.services.UserService;
import com.remix.authAPI.services.SessionService;
import com.remix.authAPI.response.ResponseHandler;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Authentifier l'utilisateur
            User user = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
            
            // Créer une session pour l'utilisateur
            Session session = sessionService.createSession(user);
            
            // Préparer la réponse
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", session.getToken());
            responseData.put("expiresAt", session.getExpiresAt());
            responseData.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail()
            ));
            
            return new ResponseHandler().generateSuccessResponse(responseData);
        } catch (RuntimeException e) {
            return new ResponseHandler().generateErrorResponse(
                e.getMessage(), 
                HttpStatus.UNAUTHORIZED
            );
        } catch (Exception e) {
            return new ResponseHandler().generateErrorResponse(
                "Erreur lors de la connexion", 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
} 