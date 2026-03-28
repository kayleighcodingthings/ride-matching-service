package com.ecabs.ridematching.service;

import com.ecabs.ridematching.domain.Driver;
import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.domain.Ride;
import com.ecabs.ridematching.exception.DriverNotFoundException;
import com.ecabs.ridematching.exception.NoDriverAvailableException;
import com.ecabs.ridematching.exception.RideNotFoundException;
import com.ecabs.ridematching.store.DriverStore;
import com.ecabs.ridematching.store.RideStore;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RideMatchingService {
    private final DriverStore driverStore;
    private final RideStore rideStore;

    public RideMatchingService(DriverStore driverStore, RideStore rideStore) {
        this.driverStore = driverStore;
        this.rideStore = rideStore;
    }

    // -------------------------------------------------------------------------
    // Driver operations
    // -------------------------------------------------------------------------

    /**
     * Registers a new driver with their initial location.
     */
    public Driver registerDriver(String name, Location location) {
        Driver driver = new Driver(name, location);
        return driverStore.save(driver);
    }

    /**
     * Updates an existing driver's location and availability.
     */
    public Driver updateDriver(UUID driverId, Location location, boolean available) {
        Driver driver = driverStore.findById(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));

        driver.setLocation(location);
        if (available) {
            driver.release();
        } else {
            driver.tryAllocateDriver();
        }

        return driverStore.save(driver);
    }

    /**
     * Returns the {@code count} nearest available drivers to the given location,
     * ordered by ascending Euclidean distance.
     */
    public List<Driver> getNearestAvailableDrivers(Location location, int count) {
        return rankAvailableDriversByDistance(location).stream()
                .limit(count)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Ride operations
    // -------------------------------------------------------------------------

    /**
     * Requests a ride for the given pickup location.
     *
     * <p>Uses a lock-free optimistic allocation loop: sorts available drivers by
     * distance, then attempts to CAS-claim each one in order until one succeeds
     * or the list is exhausted.
     *
     * @throws NoDriverAvailableException if no driver can be allocated
     */
    public Ride requestRide(Location pickupLocation) {
        for (Driver driver : rankAvailableDriversByDistance(pickupLocation)) {
            if (driver.tryAllocateDriver()) {
                Ride ride = new Ride(driver, pickupLocation);
                return rideStore.save(ride);
            }
        }
        throw new NoDriverAvailableException("No drivers are currently available for pickup at " + pickupLocation);
    }

    /**
     * Marks a ride as completed and releases its driver back to the available pool.
     *
     * @throws RideNotFoundException if no ride exists with the given ID
     * @throws IllegalStateException if the ride is already completed
     */
    public Ride completeRide(UUID rideId) {
        Ride ride = rideStore.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));
        ride.complete();
        return rideStore.save(ride);
    }

    private List<Driver> rankAvailableDriversByDistance(Location location) {
        return driverStore.findAll().stream()
                .filter(Driver::isAvailable)
                .sorted(Comparator.comparingDouble(d -> d.getLocation().distanceTo(location)))
                .toList();
    }
}
