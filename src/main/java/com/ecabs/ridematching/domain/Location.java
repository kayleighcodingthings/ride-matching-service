package com.ecabs.ridematching.domain;

/**
 * Immutable value object representing geographic coordinates.
 * Distance calculations use straight-line Euclidean distance.
 */
public record Location(double latitude, double longitude) {

    /**
     * Calculates the Euclidean (straight-line) distance between this location and the destination.
     *
     * @param destination the target location
     * @return Euclidean distance in coordinate units
     */
    public double distanceTo(Location destination) {
        double deltaLatitude = this.latitude - destination.latitude();
        double deltaLongitude = this.longitude - destination.longitude();

        return Math.sqrt(deltaLatitude * deltaLatitude + deltaLongitude * deltaLongitude);
    }

    @Override
    public String toString() {
        return "Location{lat=" + latitude + ", lng=" + longitude + "}";
    }
}
