# Ride Matching Service

A concurrent ride matching backend service built with **Java 21** and **Spring Boot 3**.

Drivers register their availability, riders request a ride and are matched with the nearest available driver, and
completed rides return the driver to the pool – all safely under concurrent load.

---

## Requirements

| Tool  | Version                   |
|-------|---------------------------|
| Java  | 21+                       |
| Maven | 3.9+ (or use the wrapper) |

---

## Running the service

```bash
# Clone and enter the project
git clone https://github.com/kayleighcodingthings/ride-matching-service.git
cd ride-matching-service

# Build and run
./mvnw spring-boot:run
```

The service starts on **http://localhost:8080**.

---

## Testing the API manually

HTTP request files are included in the `requests/` folder, compatible with **IntelliJ IDEA** (built-in) and **VS Code
** ([REST Client extension](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)):

| File                       | Description                                  |
|----------------------------|----------------------------------------------|
| `requests/drivers.http`    | Register, update, and query drivers          |
| `requests/rides.http`      | Request and complete rides, validation cases |
| `requests/end-to-end.http` | Full scenario — run steps in order           |

Replace `{{aliceId}}`, `{{rideId}}`, and `{{aliceRideId}}` with real IDs from previous responses.

---

## Running the tests

```bash
./mvnw test
```

The test suite includes:

- Unit tests covering all controller behaviours
- Unit tests covering all service behaviours
- Unit tests covering all exception handlers
- **Concurrent allocation tests** — repeated 10× with 200 simultaneous ride requests to validate thread safety under
  load

---

## API Reference

### Register a driver

```bash
curl -s -X POST http://localhost:8080/drivers \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "latitude": 51.5074, "longitude": -0.1278}' | jq
```

```json
{
  "id": "b3f1c2d4-...",
  "name": "Alice",
  "location": {
    "latitude": 51.5074,
    "longitude": -0.1278
  },
  "status": "AVAILABLE"
}
```

---

### Update driver location / availability

```bash
curl -s -X PUT http://localhost:8080/drivers/{id}/location \
  -H "Content-Type: application/json" \
  -d '{"latitude": 51.51, "longitude": -0.13, "available": true}' | jq
```

---

### Request a ride

```bash
curl -s -X POST http://localhost:8080/rides \
  -H "Content-Type: application/json" \
  -d '{"pickupLatitude": 51.5080, "pickupLongitude": -0.1280}' | jq
```

```json
{
  "id": "a1b2c3d4-...",
  "status": "ACTIVE",
  "pickupLocation": {
    "latitude": 51.508,
    "longitude": -0.128
  },
  "driver": {
    "id": "b3f1c2d4-...",
    "name": "Alice",
    "location": {
      "latitude": 51.5074,
      "longitude": -0.1278
    },
    "status": "BUSY"
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "completedAt": null
}
```

Returns `503 Service Unavailable` if no drivers are available.

---

### Complete a ride

```bash
curl -s -X PATCH http://localhost:8080/rides/{rideId}/complete | jq
```

```json
{
  "id": "a1b2c3d4-...",
  "status": "COMPLETED",
  "pickupLocation": {
    "latitude": 51.508,
    "longitude": -0.128
  },
  "driver": {
    "id": "b3f1c2d4-...",
    "name": "Alice",
    "location": {
      "latitude": 51.5074,
      "longitude": -0.1278
    },
    "status": "AVAILABLE"
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "completedAt": "2024-01-15T10:45:00Z"
}
```

---

### Get nearest available drivers

```bash
curl -s "http://localhost:8080/drivers/nearby?lat=51.508&lng=-0.128&limit=3" | jq
```

Returns up to `limit` available drivers sorted by **ascending Euclidean distance**.

---

## End-to-end walkthrough

The following sequence exercises every endpoint:

```bash
BASE=http://localhost:8080

# 1. Register two drivers at different distances from our pickup point
ALICE=$(curl -s -X POST $BASE/drivers \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","latitude":51.50,"longitude":-0.12}' | jq -r '.id')

BOB=$(curl -s -X POST $BASE/drivers \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob","latitude":51.52,"longitude":-0.10}' | jq -r '.id')

echo "Alice: $ALICE"
echo "Bob:   $BOB"

# 2. Check nearby drivers — Alice should appear first (closer to pickup)
curl -s "$BASE/drivers/nearby?lat=51.505&lng=-0.125&limit=5" | jq '[.[] | {name, status}]'

# 3. Request a ride — should allocate Alice (nearest)
RIDE=$(curl -s -X POST $BASE/rides \
  -H "Content-Type: application/json" \
  -d '{"pickupLatitude":51.505,"pickupLongitude":-0.125}')

RIDE_ID=$(echo $RIDE | jq -r '.id')
echo "Ride created: $RIDE_ID"
echo $RIDE | jq '{driver: .driver.name, status}'

# 4. Alice is now BUSY — a second request should allocate Bob
RIDE2=$(curl -s -X POST $BASE/rides \
  -H "Content-Type: application/json" \
  -d '{"pickupLatitude":51.505,"pickupLongitude":-0.125}')
echo $RIDE2 | jq '{driver: .driver.name, status}'

# 5. Complete the first ride — Alice returns to the pool
curl -s -X PATCH $BASE/rides/$RIDE_ID/complete | jq '{status, completedAt}'

# 6. Alice is available again
curl -s "$BASE/drivers/nearby?lat=51.505&lng=-0.125&limit=5" | jq '[.[] | {name, status}]'
```

---

## Architecture

The service follows a layered architecture separating HTTP concerns, business logic, and in-memory persistence.

```
┌─────────────────────────────────────────┐
│         REST Controllers                │  ← Input validation, HTTP mapping
│  DriverController  │  RideController    │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         RideMatchingService             │  ← Business logic, allocation loop
└────────────┬─────────────┬──────────────┘
             │             │
┌────────────▼──┐   ┌──────▼────────────┐
│  DriverStore  │   │    RideStore      │  ← ConcurrentHashMap, in-memory
└───────────────┘   └───────────────────┘
```

### Concurrency design

The core invariant is: **a driver can only be allocated to one ride at a time**, even under concurrent requests.

This is achieved without a global lock via a **lock-free optimistic allocation loop**:

1. **Snapshot + rank** — all available drivers are read from the `ConcurrentHashMap` and sorted by distance. This is a
   non-blocking read.
2. **CAS claim** — the service iterates the sorted candidates and calls `driver.tryAllocate()`, which uses
   `AtomicReference.compareAndSet(AVAILABLE → BUSY)`. The first thread to win the CAS proceeds; any concurrent thread
   that loses skips to the next candidate.

This means:

- No thread ever blocks waiting for another thread's lock.
- Under contention, threads fan out across the candidate list rather than queuing.
- The worst case is O(k) CAS retries where k is the number of competing threads.

---

## Design decisions

| Decision                                          | Rationale                                                                                                                 |
|---------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `AtomicReference<DriverStatus>` on `Driver`       | CAS semantics eliminate the need for an external lock during allocation                                                   |
| `ConcurrentHashMap` for both stores               | Segment-level locking allows high read concurrency with safe writes                                                       |
| `synchronized` on `Ride.complete()`               | Prevents double-completion of the same ride from concurrent calls                                                         |
| Spring Boot 3 + Java 21 records                   | Idiomatic, minimal boilerplate, clean DTO layer                                                                           |
| RFC 7807 `ProblemDetail` error responses          | Standard error envelope without a custom DTO                                                                              |
| `@RepeatedTest(10)` on concurrency tests          | Repeated execution surfaces race conditions that only manifest under specific scheduling                                  |
| `Store` over `Repository` / `domain` over `model` | Avoids implying Spring Data / JPA semantics that aren't present; aligns with DDD vocabulary for in-memory implementations |

---

## Error responses

All errors follow [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807):

```json
{
  "type": "/errors/no-driver-available",
  "title": "No Driver Available",
  "status": 503,
  "detail": "No drivers are currently available for pickup at Location{lat=51.5, lng=-0.12}"
}
```

| Scenario                             | Status |
|--------------------------------------|--------|
| No drivers available                 | `503`  |
| Driver / ride not found              | `404`  |
| Completing an already-completed ride | `409`  |
| Missing / invalid request fields     | `400`  |

---

## Future Improvements

While the current implementation focuses on correctness and concurrency, the following enhancements would be considered in a production environment:

- Using PostgreSQL with geospatial indexing for efficient proximity queries
- Introducing Redis to cache nearby drivers
- Emitting ride events via Kafka for asynchronous processing
- Using distributed locking for multi-instance deployments
- Adding driver location streaming for real-time updates