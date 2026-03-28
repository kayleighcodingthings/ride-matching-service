package com.ecabs.ridematching.controller;

import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.domain.Ride;
import com.ecabs.ridematching.service.RideMatchingService;
import com.ecabs.ridematching.controller.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/rides")
public class RideController {
    private final RideMatchingService rideMatchingService;

    public RideController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    /**
     * POST /rides
     * Request a ride — finds and allocates the nearest available driver.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponse requestRide(@Valid @RequestBody AllocateRideRequest request) {
        Ride ride = rideMatchingService.requestRide(
                new Location(
                        request.getPickupLocation().getLatitude(),
                        request.getPickupLocation().getLongitude())
        );

        return toResponse(ride);
    }

    @PatchMapping("/{id}/complete")
    public RideResponse completeRide(@PathVariable("id") UUID id) {
        Ride ride = rideMatchingService.completeRide(id);
        return toResponse(ride);
    }

    private RideResponse toResponse(Ride ride) {
        return new RideResponse(
                ride.getId().toString(),
                ride.getStatus().name(),
                new LocationResponse(
                        ride.getPickupLocation().latitude(),
                        ride.getPickupLocation().longitude()
                ),
                new DriverResponse(
                        ride.getDriver().getId().toString(),
                        ride.getDriver().getName(),
                        new LocationResponse(
                                ride.getDriver().getLocation().latitude(),
                                ride.getDriver().getLocation().longitude()
                        ),
                        ride.getDriver().getStatus().name()
                ),
                ride.getCreatedAt().toString(),
                ride.getCompletedAt() != null ? ride.getCompletedAt().toString() : null
        );
    }
}
