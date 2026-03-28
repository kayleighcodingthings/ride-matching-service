package com.ecabs.ridematching.store;

import com.ecabs.ridematching.domain.Ride;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for {@link Ride}.
 *
 * <p>If persistence is introduced, extract a DriverRepository interface and provide a database-backed implementation
 * alongside this one.
 */
@Repository
public class RideStore {
    private final ConcurrentHashMap<UUID, Ride> rides = new ConcurrentHashMap<>();

    public Ride save(Ride ride) {
        rides.put(ride.getId(), ride);
        return ride;
    }

    public Optional<Ride> findById(UUID id) {
        return Optional.ofNullable(rides.get(id));
    }

    public Collection<Ride> findAll() {
        return rides.values();
    }
}
