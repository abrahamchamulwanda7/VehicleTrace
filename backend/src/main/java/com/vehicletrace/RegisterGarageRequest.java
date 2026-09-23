package com.vehicletrace;

public record RegisterGarageRequest(
        String garageName,
        String province,
        String address,
        String phone,
        String fullName,
        String username,
        String password
) {
}