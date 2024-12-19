package com.remix.authAPI.controllers;

import com.remix.authAPI.entity.User;
import com.remix.authAPI.services.UserService;
import com.remix.authAPI.response.ResponseHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UserController extends ResponseHandler {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Liste des utilisateurs", 
              description = "Récupère la liste de tous les utilisateurs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    })
    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        List<User> users = userService.findAll();
        return generateSuccessResponse(users);
    }

    @Operation(summary = "Détails d'un utilisateur", 
              description = "Récupère les détails d'un utilisateur par son ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getUserById(
        @Parameter(description = "ID de l'utilisateur") 
        @PathVariable Long id
    ) {
        return userService.findById(id)
                .map(this::generateSuccessResponse)
                .orElse(generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Création d'un utilisateur", 
              description = "Crée un nouvel utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody User user) {
        try {
            User savedUser = userService.save(user);
            return generateSuccessResponse(savedUser);
        } catch (Exception e) {
            return generateErrorResponse("Erreur lors de la création de l'utilisateur", HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Mise à jour d'un utilisateur", 
              description = "Met à jour les informations d'un utilisateur existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour avec succès"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUser(
        @Parameter(description = "ID de l'utilisateur") 
        @PathVariable Long id,
        @RequestBody User user
    ) {
        return userService.findById(id)
                .map(existingUser -> {
                    user.setId(id);
                    User updatedUser = userService.updateUser(user);
                    return generateSuccessResponse(updatedUser);
                })
                .orElse(generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Suppression d'un utilisateur", 
              description = "Supprime un utilisateur existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(
        @Parameter(description = "ID de l'utilisateur") 
        @PathVariable Long id
    ) {
        if (userService.findById(id).isPresent()) {
            userService.deleteById(id);
            return generateSuccessResponse("Utilisateur supprimé avec succès");
        }
        return generateErrorResponse("Utilisateur non trouvé", HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Vérification des tentatives de connexion", 
              description = "Vérifie si le nombre maximum de tentatives de connexion a été dépassé")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vérification effectuée avec succès")
    })
    @GetMapping("/check-login-attempts/{email}")
    public ResponseEntity<Object> checkLoginAttempts(
        @Parameter(description = "Email de l'utilisateur") 
        @PathVariable String email
    ) {
        boolean isExceeded = userService.isLoginAttemptsExceeded(email);
        return generateSuccessResponse(Map.of(
            "isExceeded", isExceeded,
            "email", email
        ));
    }
} 