package com.remix.authAPI.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.remix.authAPI.response.ResponseHandler;
import com.remix.authAPI.services.UserService;
import com.remix.authAPI.models.User;

@RestController
@RequestMapping("/api/auth")
public class InscriptionController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User user) {
        try {
            User savedUser = userService.registerUser(user);
            return new ResponseHandler().generateSuccessResponse(savedUser);
        } catch (RuntimeException e) {
            return new ResponseHandler().generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseHandler().generateErrorResponse("Erreur lors de l'inscription", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return new ResponseHandler().generateSuccessResponse("Email vérifié avec succès");
        } catch (RuntimeException e) {
            return new ResponseHandler().generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseHandler().generateErrorResponse("Erreur lors de la vérification", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Object> test() {
        return new ResponseHandler().generateSuccessResponse("L'API fonctionne correctement");
    }
}
