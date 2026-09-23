package com.vehicletrace;

public record LoginRequest(
        String username,
        String password
) {
}