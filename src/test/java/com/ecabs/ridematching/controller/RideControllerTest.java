package com.ecabs.ridematching.controller;

import com.ecabs.ridematching.domain.Driver;
import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.domain.Ride;
import com.ecabs.ridematching.exception.GlobalExceptionHandler;
import com.ecabs.ridematching.exception.NoDriverAvailableException;
import com.ecabs.ridematching.exception.RideNotFoundException;
import com.ecabs.ridematching.service.RideMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RideController")
class RideControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideMatchingService rideMatchingService;

    private Driver driver;
    private Ride ride;

    @BeforeEach
    void setUp() {
        driver = new Driver("Alice", new Location(51.50, -0.12));
        driver.tryAllocateDriver(); // Driver is BUSY once assigned to a ride
        ride = new Ride(driver, new Location(51.505, -0.125));
    }

    // =========================================================================
    // POST /rides
    // =========================================================================

    @Nested
    @DisplayName("POST /rides")
    class RequestRide {

        @Test
        @DisplayName("returns 201 with ride and driver details on success")
        void returns201OnSuccess() throws Exception {
            when(rideMatchingService.requestRide(any(Location.class))).thenReturn(ride);

            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pickupLocation": { "latitude": 51.505, "longitude": -0.125 }
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.driver.name").value("Alice"))
                    .andExpect(jsonPath("$.driver.status").value("BUSY"))
                    .andExpect(jsonPath("$.pickupLocation.latitude").value(51.505))
                    .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @DisplayName("returns 503 when no drivers are available")
        void returns503WhenNoDriverAvailable() throws Exception {
            when(rideMatchingService.requestRide(any(Location.class))).thenThrow(new NoDriverAvailableException("No drivers available"));

            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pickupLocation": { "latitude": 51.505, "longitude": -0.125 }
                                    }
                                    """))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.title").value("No Driver Available"));
        }

        @Test
        @DisplayName("returns 400 when pickupLocation is missing")
        void returns400WhenPickupLocationMissing() throws Exception {
            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when latitude is missing from pickupLocation")
        void returns400WhenLatitudeMissingFromPickupLocation() throws Exception {
            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pickupLocation": { "longitude": -0.125 }
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PATCH /rides/{id}/complete
    // =========================================================================

    @Nested
    @DisplayName("PATCH /rides/{id}/complete")
    class CompleteRide {

        void returns200OnSuccess() throws Exception {
            ride.complete();
            when(rideMatchingService.completeRide(ride.getId())).thenReturn(ride);

            mockMvc.perform(patch("/rides/{id}/complete", ride.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }

        @Test
        @DisplayName("returns 404 when ride does not exist")
        void returns404WhenRideDoesNotExist() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(rideMatchingService.completeRide(unknownId))
                    .thenThrow(new RideNotFoundException(unknownId));

            mockMvc.perform(patch("/rides/{id}/complete", unknownId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Ride Not Found"));
        }

        @Test
        @DisplayName("returns 409 when ride is already completed")
        void returns409WhenRideIsAlreadyCompleted() throws Exception {
            when(rideMatchingService.completeRide(ride.getId()))
                    .thenThrow(new IllegalStateException("Ride is already completed"));

            mockMvc.perform(patch("/rides/{id}/complete", ride.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Invalid state transition"));
        }
    }
}