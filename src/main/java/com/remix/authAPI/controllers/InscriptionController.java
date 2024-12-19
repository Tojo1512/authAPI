package com.remix.authAPI.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.remix.authAPI.response.ResponseHandler;
import com.remix.authAPI.services.UserService;
import com.remix.authAPI.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Inscription", description = "API d'inscription et de vérification d'email")
public class InscriptionController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Inscription d'un nouvel utilisateur", 
              description = "Permet de créer un nouveau compte utilisateur avec vérification par email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inscription réussie",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
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

    @Operation(summary = "Vérification d'email", 
              description = "Vérifie l'email d'un utilisateur via un token envoyé par email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email vérifié avec succès"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou expiré"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(
        @Parameter(description = "Token de vérification envoyé par email") 
        @RequestParam String token
    ) {
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
