package com.ecabs.ridematching.domain;

import com.ecabs.ridematching.store.DriverStore;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a driver registered in the system.
 *
 * <p>Status transitions are managed via an {@link AtomicReference} to ensure thread-safe compare-and-set semantics,
 * preventing double-allocation of the same driver across concurrent ride requests without requiring external locks.
 *
 * <p>Location updates are guarded by a dedicated lock in {@link DriverStore}.
 */

public class Driver {

    private final UUID id;
    private final String name;
    private volatile Location location;
    private final AtomicReference<DriverStatus> status;

    public Driver(String name, Location location) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.location = location;
        this.status = new AtomicReference<>(DriverStatus.AVAILABLE);
    }

    /**
     * Attempts to transition this driver from AVAILABLE to BUSY atomically.
     *
     * @return {@code true} if the transition was successful, the driver was AVAILABLE and is now BUSY,
     * {@code false} if the transition failed, the driver was already BUSY
     */
    public boolean tryAllocateDriver() {
        return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.BUSY);
    }

    public void release() {
        status.set(DriverStatus.AVAILABLE);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public DriverStatus getStatus() {
        return status.get();
    }

    public boolean isAvailable() {
        return status.get() == DriverStatus.AVAILABLE;
    }
}
