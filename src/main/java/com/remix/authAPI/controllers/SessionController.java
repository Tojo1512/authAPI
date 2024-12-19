package com.remix.authAPI.controllers;

import com.remix.authAPI.services.SessionService;
import com.remix.authAPI.response.ResponseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController extends ResponseHandler {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/validate")
    public ResponseEntity<Object> validateSession(@RequestHeader("Session-Token") String token) {
        boolean isValid = sessionService.validateAndUpdateSession(token);
        return generateSuccessResponse(isValid);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestHeader("Session-Token") String token) {
        sessionService.invalidateSession(token);
        return generateSuccessResponse("Session terminée avec succès");
    }
} 