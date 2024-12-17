package com.remix.authAPI.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ResponseHandler {
    private Object data;
    private Object meta;
    private String error;
    private HttpStatus status;

    public ResponseHandler() {
        
    }

    // Pour les réponses réussies avec données et métadonnées
    public ResponseEntity<Object> generateSuccessResponse(Object data, Object meta) {
        this.data = data;
        this.meta = meta;
        this.status = HttpStatus.OK;
        return generateResponse();
    }

    // Pour les réponses réussies avec données uniquement
    public ResponseEntity<Object> generateSuccessResponse(Object data) {
        this.data = data;
        this.status = HttpStatus.OK;
        return generateResponse();
    }

    // Pour les réponses d'erreur avec message personnalisé
    public ResponseEntity<Object> generateErrorResponse(String message, HttpStatus status) {
        this.error = message;
        this.status = status;
        return generateResponse();
    }

    // Pour les réponses d'erreur avec message et données supplémentaires
    public ResponseEntity<Object> generateErrorResponse(String message, HttpStatus status, Object errorDetails) {
        this.error = message;
        this.status = status;
        this.data = errorDetails;
        return generateResponse();
    }

    private ResponseEntity<Object> generateResponse() {
        Map<String, Object> response = new HashMap<>();
        
        if (data != null) {
            response.put("data", data);
        }
        
        if (error != null) {
            response.put("error", error);
        }
        
        response.put("code", status.value());
        response.put("status", status.getReasonPhrase());
        
        if (meta != null) {
            response.put("meta", meta);
        }

        return new ResponseEntity<>(response, status);
    }
}