package com.vehicletrace;

public record VehicleRequest(
        String numberPlate,
        String make,
        String model,
        String ownerName,
        String ownerPhone
) {
}