package com.vehicletrace;

public record CreateUserRequest(
        String fullName,
        String username,
        String password,
        String role
) {
}