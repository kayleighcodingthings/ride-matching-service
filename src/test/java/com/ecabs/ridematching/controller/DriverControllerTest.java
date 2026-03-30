package com.ecabs.ridematching.controller;

import com.ecabs.ridematching.domain.Driver;
import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.exception.DriverNotFoundException;
import com.ecabs.ridematching.exception.GlobalExceptionHandler;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("DriverController")
class DriverControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideMatchingService rideMatchingService;

    private Driver driver;

    @BeforeEach
    void setUp() {
        driver = new Driver("Alice", new Location(51.50, -0.12));
    }

    @Nested
    @DisplayName("POST /drivers")
    class RegisterDriver {
        @Test
        @DisplayName("returns 201 with driver details on success")
        void returns201OnSuccess() throws Exception {
            when(rideMatchingService.registerDriver(eq("Alice"), any(Location.class)))
                    .thenReturn(driver);

            mockMvc.perform(post("/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Alice",
                                      "location": {
                                        "latitude": 51.50,
                                        "longitude": -0.12
                                      }
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.location.latitude").value(51.50))
                    .andExpect(jsonPath("$.location.longitude").value(-0.12));
        }

        @Test
        @DisplayName("returns 400 when name is missing")
        void returns400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "location": { "latitude": 51.50, "longitude": -0.12 }
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when location is missing")
        void returns400WhenLocationMissing() throws Exception {
            mockMvc.perform(post("/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Alice" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when latitude is missing from location")
        void returns400WhenLatitudeMissing() throws Exception {
            mockMvc.perform(post("/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Alice",
                                      "location": { "longitude": -0.12 }
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /drivers/{id}/location")
    class UpdateDriver {
        @Test
        @DisplayName("returns 200 with updated driver on success")
        void returns200OnSuccess() throws Exception {
            when(rideMatchingService.updateDriver(eq(driver.getId()), any(Location.class), eq(true)))
                    .thenReturn(driver);

            mockMvc.perform(put("/drivers/{id}/location", driver.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "location": {
                                        "latitude": 51.51,
                                        "longitude": -0.13
                                      },
                                      "available": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Alice"));
        }

        @Test
        @DisplayName("returns 404 when driver does not exist")
        void returns404WhenDriverNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(rideMatchingService.updateDriver(eq(unknownId), any(Location.class), anyBoolean()))
                    .thenThrow(new DriverNotFoundException(unknownId));

            mockMvc.perform(put("/drivers/{id}/location", unknownId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "location": {
                                        "latitude": 51.51,
                                        "longitude": -0.13
                                      },
                                      "available": true
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when location is missing")
        void returns400WhenLocationMissing() throws Exception {
            mockMvc.perform(put("/drivers/{id}/location", driver.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "available": true }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /drivers/nearby")
    class GetNearbyDrivers {
        @Test
        @DisplayName("returns 200 with sorted driver list")
        void returns200WithDriverList() throws Exception {
            Driver driver2 = new Driver("Bob", new Location(51.52, -0.10));
            when(rideMatchingService.getNearestAvailableDrivers(any(Location.class), eq(2)))
                    .thenReturn(List.of(driver, driver2));

            mockMvc.perform(get("/drivers/nearby")
                            .param("latitude", "51.505")
                            .param("longitude", "-0.125")
                            .param("count", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Alice"))
                    .andExpect(jsonPath("$[1].name").value("Bob"));
        }

        @Test
        @DisplayName("returns 400 when count is missing")
        void returns400WhenCountMissing() throws Exception {
            mockMvc.perform(get("/drivers/nearby")
                            .param("latitude", "51.505")
                            .param("longitude", "-0.125"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when count is less than 1")
        void returns400WhenCountBelowMinimum() throws Exception {
            mockMvc.perform(get("/drivers/nearby")
                            .param("latitude", "51.505")
                            .param("longitude", "-0.125")
                            .param("count", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns empty list when no drivers are available")
        void returnsEmptyListWhenNoneAvailable() throws Exception {
            when(rideMatchingService.getNearestAvailableDrivers(any(Location.class), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/drivers/nearby")
                            .param("latitude", "51.505")
                            .param("longitude", "-0.125")
                            .param("count", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
