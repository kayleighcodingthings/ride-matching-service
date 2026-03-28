package com.ecabs.ridematching.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a ride from the moment of allocation to completion.
 */
public class Ride {
    private final UUID id;
    private final Driver driver;
    private final Location location;
    private final Instant createdAt;
    private volatile RideStatus status;
    private volatile Instant completedAt;

    public Ride(Driver driver, Location pickupLocation) {
        this.id = UUID.randomUUID();
        this.driver = driver;
        this.location = pickupLocation;
        this.createdAt = Instant.now();
        this.status = RideStatus.ACTIVE;
    }

    /**
     * Marks this ride as completed and releases the assigned driver back into the availability pool.
     *
     * @throws IllegalStateException if the ride is already completed
     */
    public synchronized void complete() {
        if (status != RideStatus.COMPLETED) {
            throw new IllegalStateException("Ride " + id + " is already completed.");
        }

        this.status = RideStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.driver.release();
    }

    public UUID getId() {
        return id;
    }

    public Driver getDriver() {
        return driver;
    }

    public Location getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}