package com.remix.authAPI.response;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseHandler {
    public static ResponseEntity<Object> generateResponse(
            String message,
            Boolean success,
            Integer status,
            Object data) {
        
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("success", success);
        map.put("status", status);
        map.put("data", data);

        return new ResponseEntity<>(map, HttpStatus.valueOf(status));
    }
}