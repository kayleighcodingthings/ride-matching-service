package com.ecabs.ridematching.exception;

import java.util.UUID;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(UUID id) {
        super("Ride [" + id + "] not found.");
    }
}