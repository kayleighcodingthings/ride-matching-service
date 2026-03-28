package com.ecabs.ridematching.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request and response DTOs, kept in one file for brevity.
 * In a larger codebase these would live in a dedicated dto package.
 */
public final class Dtos {
    private Dtos() {
    }

    // --- Shared ---
    public record LocationDto(
            @NotNull(message = "Latitude is required") Double latitude,
            @NotNull(message = "Longitude is required") Double longitude
    ) {
    }

    // --- Request DTOs ---

    public record RegisterDriverRequest(
            @NotBlank(message = "Driver name is required") String name,
            @NotNull(message = "location is required") @Valid LocationDto location
    ) {
    }

    public record UpdateDriverRequest(
            @NotNull(message = "location is required") @Valid LocationDto location,
            boolean available
    ) {
    }

    public record AllocateRideRequest(
            @NotNull(message = "pickupLocation is required") @Valid LocationDto pickupLocation
    ) {
    }

    // --- Response DTOs ---

    public record LocationResponse(double latitude, double longitude) {}

    public record DriverResponse(
            String id,
            String name,
            LocationResponse location,
            String status
    ) {}

    public record RideResponse(
            String id,
            String status,
            LocationResponse pickupLocation,
            DriverResponse driver,
            String createdAt,
            String completedAt
    ) {}
}