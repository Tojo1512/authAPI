package com.remix.authAPI.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.remix.authAPI.dto.LoginRequest;
import com.remix.authAPI.dto.TwoFactorVerificationRequest;
import com.remix.authAPI.entity.User;
import com.remix.authAPI.entity.Session;
import com.remix.authAPI.services.UserService;
import com.remix.authAPI.services.SessionService;
import com.remix.authAPI.response.ResponseHandler;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "API d'authentification avec 2FA")
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @Operation(summary = "Initiation de la connexion", 
              description = "Première étape de connexion qui déclenche l'envoi du code 2FA")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Code 2FA envoyé avec succès"),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @PostMapping("/login/initiate")
    public ResponseEntity<Object> initiateLogin(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userService.initiateLogin(loginRequest.getEmail(), loginRequest.getPassword());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Code de vérification envoyé par email");
            responseData.put("email", user.getEmail());
            
            return new ResponseHandler().generateSuccessResponse(responseData);
        } catch (RuntimeException e) {
            return new ResponseHandler().generateErrorResponse(
                e.getMessage(), 
                HttpStatus.UNAUTHORIZED
            );
        }
    }

    @Operation(summary = "Vérification du code 2FA", 
              description = "Seconde étape de connexion qui vérifie le code 2FA")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification réussie",
            content = @Content(schema = @Schema(implementation = Session.class))),
        @ApiResponse(responseCode = "401", description = "Code 2FA invalide"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @PostMapping("/login/verify")
    public ResponseEntity<Object> verifyTwoFactor(@RequestBody TwoFactorVerificationRequest request) {
        try {
            User user = userService.verifyTwoFactorCode(request.getEmail(), request.getPin());
            
            // Créer une session pour l'utilisateur
            Session session = sessionService.createSession(user);
            
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
        }
    }
} 