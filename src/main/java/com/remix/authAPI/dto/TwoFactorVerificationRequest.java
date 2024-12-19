package com.remix.authAPI.dto;

import lombok.Data;

@Data
public class TwoFactorVerificationRequest {
    private String email;
    private String pin;
} 