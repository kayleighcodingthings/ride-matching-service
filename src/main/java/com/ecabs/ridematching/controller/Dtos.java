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

    public static class LocationDto {
        @NotNull(message = "Latitude is required") private Double latitude;
        @NotNull(message = "Longitude is required") private Double longitude;

        public LocationDto() {}

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    // --- Request DTOs ---

    public static class RegisterDriverRequest {
        @NotBlank(message = "Driver name is required") private String name;
        @NotNull(message = "Location is required") @Valid private LocationDto location;

        public RegisterDriverRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocationDto getLocation() { return location; }
        public void setLocation(LocationDto location) { this.location = location; }
    }

    public static class UpdateDriverRequest {
        @NotNull(message = "Location is required") @Valid private LocationDto location;
        private boolean available;

        public UpdateDriverRequest() {}

        public LocationDto getLocation() { return location; }
        public void setLocation(LocationDto location) { this.location = location; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }

    public static class AllocateRideRequest {
        @NotNull(message = "pickupLocation is required") @Valid private LocationDto pickupLocation;

        public AllocateRideRequest() {}

        public LocationDto getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(LocationDto pickupLocation) { this.pickupLocation = pickupLocation; }
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