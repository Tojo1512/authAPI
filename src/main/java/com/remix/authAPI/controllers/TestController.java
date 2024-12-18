package com.remix.authAPI.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remix.authAPI.response.ResponseHandler;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/")
    public ResponseEntity<Object> test() {
        return new ResponseHandler().generateSuccessResponse("L'API fonctionne correctement");
    }
}
