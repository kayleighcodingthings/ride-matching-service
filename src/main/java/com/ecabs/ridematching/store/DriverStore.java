package com.ecabs.ridematching.store;

import com.ecabs.ridematching.domain.Driver;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for {@link Driver}.
 *
 * <p>Read operations are non-blocking thanks to {@link ConcurrentHashMap}'s segment level-locking. Write operations are safe
 * because the map guarantees atomicity at the key level, and {@link Driver} guards its own mutable fields.
 */
@Repository
public class DriverStore {
    private final ConcurrentHashMap<UUID, Driver> drivers = new ConcurrentHashMap<>();

    public Driver save(Driver driver) {
        drivers.put(driver.getId(), driver);
        return driver;
    }

    public Optional<Driver> findById(UUID id) {
        return Optional.ofNullable(drivers.get(id));
    }

    public Collection<Driver> findAll() {
        return drivers.values();
    }

    public boolean exists(UUID id) {
        return drivers.containsKey(id);
    }
}
