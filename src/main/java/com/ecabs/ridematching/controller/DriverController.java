package com.ecabs.ridematching.controller;

import com.ecabs.ridematching.domain.Driver;
import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.service.RideMatchingService;
import com.ecabs.ridematching.controller.Dtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/drivers")
@Validated
public class DriverController {
    private final RideMatchingService rideMatchingService;

    public DriverController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    /**
     * POST /drivers
     * Register a new driver with their name and current location.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse registerDriver(@Valid @RequestBody RegisterDriverRequest request) {
        Driver driver = rideMatchingService.registerDriver(
                request.name(),
                new Location(request.location().latitude(), request.location().longitude())
        );

        return toResponse(driver);
    }

    /**
     * PUT /drivers/{id}/location
     * Update a driver's location and/or availability.
     */
    @PutMapping("/{id}/location")
    public DriverResponse updateDriver(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateDriverRequest request) {
        Driver driver = rideMatchingService.updateDriver(
                id,
                new Location(request.location().latitude(), request.location().longitude()),
                request.available()
        );
        return toResponse(driver);
    }

    /**
     * GET /drivers/nearby?lat=&lng=&limit=
     * Returns the nearest X available drivers, ascending by distance.
     */
    @GetMapping("/nearby")
    public List<DriverResponse> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") @Min(1) int limit) {
        return rideMatchingService
                .getNearestAvailableDrivers(new Location(lat, lng), limit)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DriverResponse toResponse(Driver driver) {
        return new DriverResponse(
                driver.getId().toString(),
                driver.getName(),
                new LocationResponse(
                        driver.getLocation().latitude(),
                        driver.getLocation().longitude()
                ),
                driver.getStatus().name()
        );
    }
}
