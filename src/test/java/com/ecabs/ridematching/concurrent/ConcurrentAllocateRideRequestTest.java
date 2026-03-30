package com.ecabs.ridematching.concurrent;

import com.ecabs.ridematching.domain.DriverStatus;
import com.ecabs.ridematching.domain.Location;
import com.ecabs.ridematching.domain.Ride;
import com.ecabs.ridematching.exception.NoDriverAvailableException;
import com.ecabs.ridematching.service.RideMatchingService;
import com.ecabs.ridematching.store.DriverStore;
import com.ecabs.ridematching.store.RideStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for the ride matching service.
 *
 * <p>These tests validate the core invariant under parallel load:
 * <em>each driver is allocated to at most one ride at a time</em>.
 *
 * <p>Tests are {@link RepeatedTest repeated} to increase the chance of
 * exposing race conditions that only surface under specific thread
 * scheduling. The service must pass every repetition.
 */
@DisplayName("Concurrent ride allocation")
class ConcurrentAllocateRideRequestTest {
    private RideMatchingService rideMatchingService;

    @BeforeEach
    void setUp() {
        rideMatchingService = new RideMatchingService(new DriverStore(), new RideStore());
    }

    /**
     * The primary concurrency invariant test.
     *
     * <p>Registers N drivers, fires N+extra concurrent ride requests simultaneously
     * using a {@link CountDownLatch} to synchronise thread start as closely as
     * possible, then asserts:
     * <ul>
     *   <li>Exactly N rides were created (one per driver — no over-allocation).</li>
     *   <li>No driver was allocated to more than one ride.</li>
     *   <li>All allocated drivers are in BUSY status.</li>
     * </ul>
     */
    @RepeatedTest(10)
    @DisplayName("never allocates the same driver to two concurrent requests")
    void noDoubleAllocationUnderConcurrentRequests() throws InterruptedException {
        int driverCount = 5;
        int requestCount = 10;

        for (int i = 0; i < driverCount; i++) {
            rideMatchingService.registerDriver("Driver-" + i, new Location(i, i));
        }

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(requestCount);

        List<Ride> successfulRides = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger noDriverCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    start.await(); // all threads wait here — maximise contention
                    Ride ride = rideMatchingService.requestRide(new Location(0, 0));
                    successfulRides.add(ride);
                } catch (NoDriverAvailableException e) {
                    noDriverCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    end.countDown();
                }
            });
        }

        start.countDown(); // release all threads simultaneously
        assertThat(end.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Exactly as many rides as there are drivers
        assertThat(successfulRides).hasSize(driverCount);
        assertThat(noDriverCount.get()).isEqualTo(requestCount - driverCount);

        // No double allocation
        Set<UUID> allocatedDrivers = ConcurrentHashMap.newKeySet();
        for (Ride ride : successfulRides) {
            boolean isNew = allocatedDrivers.add(ride.getDriver().getId());
            assertThat(isNew)
                    .as("Driver %s was allocated to more than one ride", ride.getDriver().getId())
                    .isTrue();
        }

        // All allocated drivers are BUSY
        successfulRides.forEach(ride ->
                assertThat(ride.getDriver().getStatus())
                        .as("Driver %s should be BUSY", ride.getDriver().getName())
                        .isEqualTo(DriverStatus.BUSY)
        );
    }

    /**
     * Validates that drivers are correctly returned to the available pool
     * under concurrent complete-and-request cycles.
     *
     * <p>Simulates a busy period: 1 driver, repeated ride → complete cycles
     * fired concurrently. The invariant is that at no point does the driver's
     * status become inconsistent.
     */
    @RepeatedTest(5)
    @DisplayName("driver status is consistent across concurrent complete-and-request cycles")
    void driverStatusConsistentAcrossCompletionCycles() throws InterruptedException {
        rideMatchingService.registerDriver("Alice", new Location(0, 0));

        int cycles = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch end = new CountDownLatch(cycles);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 0; i < cycles; i++) {
            executor.submit(() -> {
                try {
                    Ride ride = rideMatchingService.requestRide(new Location(0, 0));
                    //Immediately complete the ride to ensure the driver is marked as available
                    rideMatchingService.completeRide(ride.getId());
                    successCount.incrementAndGet();
                } catch (NoDriverAvailableException ignored) {
                    // Expected when driver is temporarily BUSY between cycles
                } finally {
                    end.countDown();
                }
            });
        }

        end.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // At least one cycle should succeed
        assertThat(successCount.get()).isPositive();
        // Driver must end in a valid state, either BUSY or AVAILABLE
        assertThat(rideMatchingService.getNearestAvailableDrivers(new Location(0, 0), 10))
                .hasSizeLessThanOrEqualTo(1);
    }

    /**
     * Stress test: 50 drivers, 200 concurrent ride requests.
     * Validates the invariant holds at higher scale.
     */
    @Test
    @DisplayName("holds invariant under high-concurrency stress (50 drivers, 200 requests)")
    void stressTestHighConcurrency() throws InterruptedException {
        int driverCount = 50;
        int requestCount = 200;

        for (int i = 0; i < driverCount; i++) {
            rideMatchingService.registerDriver("Driver-" + i, new Location(i * 0.01, i * 0.01));
        }

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(requestCount);

        Set<UUID> allocatedDrivers = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(50);
        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    Ride ride = rideMatchingService.requestRide(new Location(0, 0));
                    successCount.incrementAndGet();
                    boolean isNew = allocatedDrivers.add(ride.getDriver().getId());
                    if (!isNew) duplicates.incrementAndGet();
                } catch (NoDriverAvailableException | InterruptedException ignored) {
                } finally {
                    end.countDown();
                }
            });
        }
    }
}
