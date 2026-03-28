package com.ecabs.ridematching.exception;

import com.ecabs.ridematching.domain.Location;

public class NoDriverAvailableException extends RuntimeException {
    public NoDriverAvailableException(Location pickupLocation) {
        super("No drivers are currently available for pickup at " + pickupLocation.toString());
    }
}