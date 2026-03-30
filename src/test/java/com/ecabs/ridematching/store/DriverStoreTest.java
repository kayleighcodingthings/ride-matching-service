package com.ecabs.ridematching.store;

import com.ecabs.ridematching.domain.Driver;
import com.ecabs.ridematching.domain.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DriverStore")
class DriverStoreTest {
    private DriverStore driverStore;

    @BeforeEach
    void setUp() {
        driverStore = new DriverStore();
    }

    @Test
    @DisplayName("saves and retrieves a driver by ID")
    void savesAndRetrievesDriver() {
        Driver driver = new Driver("Alice", new Location(1.0, 2.0));
        driverStore.save(driver);

        assertThat(driverStore.findById(driver.getId()))
                .isPresent()
                .get()
                .extracting(Driver::getName)
                .isEqualTo("Alice");
    }

    @Test
    @DisplayName("returns empty Optional for unknown driver ID")
    void returnsEmptyForUnknownId() {
        assertThat(driverStore.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("findAll returns all saved drivers")
    void findAllReturnsAllDrivers() {
        driverStore.save(new Driver("Alice", new Location(1.0, 2.0)));
        driverStore.save(new Driver("Bob", new Location(3.0, 4.0)));

        assertThat(driverStore.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("overwrites a driver ID replaces the entry")
    void overwriteReplacesDriverId() {
        Driver original = new Driver("Alice", new Location(1.0, 2.0));
        driverStore.save(original);

        // Directly overwrite using same ID reference
        driverStore.save(original);

        assertThat(driverStore.findAll()).hasSize(1);
    }
}
