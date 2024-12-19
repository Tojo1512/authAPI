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

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

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