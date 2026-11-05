package com.desafio.disruption.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record ConnectionImpact(
        String arrivingSegmentId,
        String departingSegmentId,
        String connectionAirport,
        LocalDateTime projectedArrival,
        LocalDateTime connectionDeparture,
        Duration availableTime,
        boolean missed) {

    public ConnectionImpact {
        Objects.requireNonNull(arrivingSegmentId, "arrivingSegmentId");
        Objects.requireNonNull(departingSegmentId, "departingSegmentId");
        Objects.requireNonNull(connectionAirport, "connectionAirport");
        Objects.requireNonNull(projectedArrival, "projectedArrival");
        Objects.requireNonNull(connectionDeparture, "connectionDeparture");
        Objects.requireNonNull(availableTime, "availableTime");
    }
}
