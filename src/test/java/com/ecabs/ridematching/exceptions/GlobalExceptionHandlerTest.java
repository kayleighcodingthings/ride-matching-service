package com.ecabs.ridematching.exceptions;

import com.ecabs.ridematching.controller.DriverController;
import com.ecabs.ridematching.controller.RideController;
import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.exception.DriverNotFoundException;
import com.ecabs.ridematching.exception.GlobalExceptionHandler;
import com.ecabs.ridematching.exception.NoDriverAvailableException;
import com.ecabs.ridematching.service.RideMatchingService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DriverController.class, RideController.class})
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideMatchingService rideMatchingService;

    // =========================================================================
    // MethodArgumentNotValidException — @Valid on @RequestBody
    // =========================================================================

    @Nested
    @DisplayName("MethodArgumentNotValidException - @RequestBody validation")
    class RequestBodyValidation {
        @Test
        @DisplayName("returns 400 with field detail when @RequestBody field fails @NotNull")
        void returns400WithFieldDetailOnNotNull() throws Exception {
            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.detail").isNotEmpty());
        }

        @Test
        @DisplayName("returns 400 when nested field is missing from request body")
        void returns400WhenNestedFieldMissing() throws Exception {
            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(("""
                                    {
                                      "pickupLocation": { "longitude": -0.125 }
                                    }
                                    """)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.detail").isNotEmpty());
        }

        @Test
        @DisplayName("returns 400 when driver name is blank")
        void returns400WhenNameBlank() throws Exception {
            mockMvc.perform(post("/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "",
                                      "location": { "latitude": 1.0, "longitude": 1.0 }
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"));
        }
    }

    // =========================================================================
    // ConstraintViolationException — @Min on @RequestParam
    // =========================================================================

    @Nested
    @DisplayName("ConstraintViolationException - @RequestParam constraint")
    class RequestParamConstraint {
        @Test
        @DisplayName("returns 400 with field detail when @RequestParam field fails @Min")
        void returns400WithFieldDetailOnMin() throws Exception {
            mockMvc.perform(get("/drivers/nearby")
                            .param("lat", "0")
                            .param("lng", "0")
                            .param("count", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("greater than or equal to 1")));
        }

        @Test
        @DisplayName("returns 400 when count is negative")
        void returns400WithFieldDetailOnNegativeCount() throws Exception {
            mockMvc.perform(get("/drivers/nearby")
                            .param("lat", "0")
                            .param("lng", "0")
                            .param("count", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("greater than or equal to 1")));
        }
    }

    // =========================================================================
    // MissingServletRequestParameterException — required @RequestParam absent
    // =========================================================================

    @Nested
    @DisplayName("MissingServletRequestParameterException - required @RequestParam absent")
    class MissingRequestParam {
        @Test
        @DisplayName("returns 400 with field detail when required @RequestParam count is missing")
        void returns400WhenCountMissing() throws Exception {
            mockMvc.perform(get("/drivers/nearby")
                            .param("lat", "0")
                            .param("lng", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.detail").value("Required parameter 'count' is missing"));
        }

        @Test
        @DisplayName("returns 400 with field detail when required @RequestParam lat is missing")
        void returns400WhenLatMissing() throws Exception {
            mockMvc.perform(get("/drivers/nearby")
                            .param("lng", "0")
                            .param("count", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.detail").value("Required parameter 'lat' is missing"));
        }
    }

    // =========================================================================
    // Domain exceptions
    // =========================================================================


    @Nested
    @DisplayName("Domain exceptions")
    class DomainExceptions {
        @Test
        @DisplayName("returns 503 when there are no drivers available")
        void returns503WhenNoDriversAvailable() throws Exception {
            when(rideMatchingService.requestRide(any(Location.class)))
                    .thenThrow(new NoDriverAvailableException("No Drivers Available"));

            mockMvc.perform(post("/rides")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pickupLocation": {
                                        "latitude": 0.0,
                                        "longitude": 0.0
                                      }
                                    }
                                    """))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.title").value("No Driver Available"))
                    .andExpect(jsonPath("$.detail").value("No Drivers Available"));
        }

        @Test
        @DisplayName("returns 404 when driver is not found")
        void returns404WhenDriverNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(rideMatchingService.updateDriver(any(), any(), any(Boolean.class)))
                    .thenThrow(new DriverNotFoundException(unknownId));

            mockMvc.perform(put("/drivers/{id}/location", unknownId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "location": { "latitude": 1.0, "longitude": 1.0 },
                                      "available": true
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Driver Not Found"));
        }

        @Test
        @DisplayName("returns 409 on invalid state transition")
        void returns409OnInvalidState() throws Exception {
            UUID rideId = UUID.randomUUID();
            when(rideMatchingService.completeRide(rideId))
                    .thenThrow(new IllegalStateException("Ride already completed"));

            mockMvc.perform(patch("/rides/{id}/complete", rideId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Invalid state transition"))
                    .andExpect(jsonPath("$.detail").value("Ride already completed"));
        }
    }
}
