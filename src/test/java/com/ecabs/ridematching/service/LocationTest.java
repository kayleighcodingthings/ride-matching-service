package com.ecabs.ridematching.service;

import com.ecabs.ridematching.domain.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName( "Location")
class LocationTest {

    @Test
    @DisplayName("distance to self is zero")
    void distanceToSelfIsZero() {
        Location location = new Location(3.0, 4.0);
        assertThat(location.distanceTo(location)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("computes correct Euclidean distance")
    void computesCorrectEuclideanDistance() {
        Location location1 = new Location(1.0, 2.0);
        Location location2 = new Location(4.0, 6.0);
        assertThat(location1.distanceTo(location2)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("distance is symmetric")
    void distanceIsSymmetric() {
        Location location1 = new Location(1.0, 2.0);
        Location location2 = new Location(4.0, 6.0);
        assertThat(location2.distanceTo(location1)).isEqualTo(location1.distanceTo(location2));
    }

    @Test
    @DisplayName("distance non-negative")
    void distanceIsNonNegative() {
        Location location1 = new Location(-5.0, -3.0);
        Location location2 = new Location(2.0,  1.0);
        assertThat(location1.distanceTo(location2)).isGreaterThanOrEqualTo(0.0);
    }

}
