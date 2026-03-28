package com.ecabs.ridematching.service;

import com.ecabs.ridematching.domain.*;
import com.ecabs.ridematching.exception.NoDriverAvailableException;
import com.ecabs.ridematching.exception.RideNotFoundException;
import com.ecabs.ridematching.store.DriverStore;
import com.ecabs.ridematching.store.RideStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RideMatchingService")
class RideMatchingServiceTest {
    private DriverStore driverStore;
    private RideStore rideStore;
    private RideMatchingService rideMatchingService;

    @BeforeEach
    void setUp() {
        driverStore = new DriverStore();
        rideStore = new RideStore();
        rideMatchingService = new RideMatchingService(driverStore, rideStore);
    }

    // =========================================================================
    // Driver registration
    // =========================================================================

    @Nested
    @DisplayName("registerDriver")
    class RegisterDriver {
        @Test
        @DisplayName("creates a driver with AVAILABLE status")
        void createsDriverAsAvailable() {
            Driver driver = rideMatchingService.registerDriver("John", new Location(1.0, 2.0));

            assertThat(driver.getId()).isNotNull();
            assertThat(driver.getName()).isEqualTo("John");
            assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
            assertThat(driver.getLocation().latitude()).isEqualTo(1.0);
            assertThat(driver.getLocation().longitude()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("persists driver in the store")
        void persistsDriver() {
            Driver driver = rideMatchingService.registerDriver("John", new Location(1.0, 2.0));

            assertThat(driverStore.findById(driver.getId())).isPresent();
        }
    }

    // =========================================================================
    // Ride request — nearest driver selection
    // =========================================================================

    @Nested
    @DisplayName("requestRide - driver selection")
    class RequestRideSelection {
        @Test
        @DisplayName("allocates the single available driver")
        void allocatesSingleDriver() {
            rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            Ride ride = rideMatchingService.requestRide(new Location(1.0, 1.0));

            assertThat(ride).isNotNull();
            assertThat(ride.getStatus()).isEqualTo(RideStatus.ACTIVE);
            assertThat(ride.getDriver().getName()).isEqualTo("John");
        }

        @Test
        @DisplayName("selects the nearest available driver")
        void selectsNearestDriver() {
            rideMatchingService.registerDriver("Far", new Location(100.0, 100.0));
            rideMatchingService.registerDriver("Mid", new Location(50.0, 50.0));
            rideMatchingService.registerDriver("Near", new Location(1.1, 1.1));

            Ride ride = rideMatchingService.requestRide(new Location(1.0, 1.0));

            assertThat(ride.getDriver().getName()).isEqualTo("Near");
        }

        @Test
        @DisplayName("marks the allocated driver as BUSY")
        void markDriverAsBusy() {
            Driver driver = rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            rideMatchingService.requestRide(new Location(1.0, 1.0));

            assertThat(driver.getStatus()).isEqualTo(DriverStatus.BUSY);
        }

        @Test
        @DisplayName("throws NoDriverAvailableException when no drivers are available")
        void throwsWhenNoDriversToAllocate() {
            assertThatThrownBy(() -> rideMatchingService.requestRide(new Location(1.0, 1.0)))
                    .isInstanceOf(NoDriverAvailableException.class);
        }

        @Test
        @DisplayName("throws NoDriverAvailableException when all drivers are BUSY")
        void throwsWhenAllDriversAreBusy() {
            rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            rideMatchingService.requestRide(new Location(1.0, 1.0)); //John is now BUSY

            assertThatThrownBy(() -> rideMatchingService.requestRide(new Location(1.0, 1.0)))
                    .isInstanceOf(NoDriverAvailableException.class);
        }

        @Test
        @DisplayName("falls back to the second nearest driver when the nearest is busy")
        void fallsBackToSecondNearest() {
            Driver nearest = rideMatchingService.registerDriver("Nearest", new Location(1.0, 1.0));
            rideMatchingService.registerDriver("Second", new Location(2.0, 2.0));

            //Manually mark as busy
            nearest.tryAllocateDriver();

            Ride ride = rideMatchingService.requestRide(new Location(1.0, 1.0));
            assertThat(ride.getDriver().getName()).isEqualTo("Second");
        }
    }

    // =========================================================================
    // Ride completion
    // =========================================================================

    @Nested
    @DisplayName("completeRide")
    class CompleteRide {

        @Test
        @DisplayName("marks the ride as COMPLETED")
        void marksRideAsCompleted() {
            rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            Ride ride = rideMatchingService.requestRide(new Location(1.0, 1.0));

            rideMatchingService.completeRide(ride.getId());

            assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
            assertThat(ride.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("releases the driver back to the available pool")
        void releasesDriverOnComplete() {
            Driver driver = rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            Ride ride = rideMatchingService.requestRide(new Location(1.0, 1.0));

            assertThat(driver.getStatus()).isEqualTo(DriverStatus.BUSY);

            rideMatchingService.completeRide(ride.getId());

            assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
        }

        @Test
        @DisplayName("a released driver can accept a new ride")
        void releasedDriverAcceptsNewRide() {
            Driver driver = rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            Ride ride1 = rideMatchingService.requestRide(new Location(1.0, 1.0));
            rideMatchingService.completeRide(ride1.getId());

            Ride ride2 = rideMatchingService.requestRide(new Location(1.0, 1.0));
            assertThat(ride2).isNotNull();
            assertThat(ride2.getDriver().getName()).isEqualTo("John");
        }

        @Test
        @DisplayName("throws RideNotFoundException for unknown ride ID")
        void throwsForUnknownRideId() {
            assertThatThrownBy(() -> rideMatchingService.completeRide(UUID.randomUUID()))
                    .isInstanceOf(RideNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when ride is already completed")
        void throwsWhenAlreadyCompleted() {
            rideMatchingService.registerDriver("John", new Location(1.0, 1.0));
            Ride ride = rideMatchingService.requestRide(new Location(1.0, 1.0));
            rideMatchingService.completeRide(ride.getId());

            assertThatThrownBy(() -> rideMatchingService.completeRide(ride.getId()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // Nearby drivers
    // =========================================================================

    @Nested
    @DisplayName("getNearestAvailableDrivers")
    class GetNearestDrivers {
        @Test
        @DisplayName("returns ascending by distance")
        void returnsAscendingByDistance() {
            rideMatchingService.registerDriver("Far", new Location(100.0, 100.0));
            rideMatchingService.registerDriver("Mid", new Location(50.0, 50.0));
            rideMatchingService.registerDriver("Near", new Location(1.1, 1.1));

            List<Driver> drivers = rideMatchingService.getNearestAvailableDrivers(new Location(1.0, 1.0), 10);

            assertThat(drivers).hasSize(3)
                .extracting(Driver::getName)
                .containsExactly("Near", "Mid", "Far");
        }

        @Test
        @DisplayName("respects count parameter")
        void respectsCount(){
            rideMatchingService.registerDriver("A", new Location(100.0, 100.0));
            rideMatchingService.registerDriver("B", new Location(50.0, 50.0));
            rideMatchingService.registerDriver("C", new Location(1.1, 1.1));

            List<Driver> drivers = rideMatchingService.getNearestAvailableDrivers(new Location(1.0, 1.0), 2);

            assertThat(drivers).hasSize(2);
        }

        @Test
        @DisplayName("excludes busy drivers from results")
        void excludesBusyDrivers(){
            Driver busyDriver = rideMatchingService.registerDriver("Busy John", new Location(1.0, 1.0));
            rideMatchingService.registerDriver("Free John", new Location(2.0, 2.0));
            busyDriver.tryAllocateDriver();

            List<Driver> drivers = rideMatchingService.getNearestAvailableDrivers(new Location(1.0, 1.0), 10);

            assertThat(drivers).extracting(Driver::getName).containsExactly("Free John");
        }

        @Test
        @DisplayName("returns empty list when no drivers are available")
        void returnsEmptyListWhenNoDriversAvailable(){
            List<Driver> drivers = rideMatchingService.getNearestAvailableDrivers(new Location(1.0, 1.0), 10);
            assertThat(drivers).isEmpty();
        }
    }
}
