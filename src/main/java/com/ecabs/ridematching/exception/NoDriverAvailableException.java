package com.ecabs.ridematching.exception;

import com.ecabs.ridematching.domain.Location;

public class NoDriverAvailableException extends RuntimeException {
    public NoDriverAvailableException(String message) {
        super(message);
    }
}