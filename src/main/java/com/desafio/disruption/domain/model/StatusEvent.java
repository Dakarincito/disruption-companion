package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.util.Objects;

public record StatusEvent(
        String eventId,
        String segmentId,
        String source,
        Instant recordedAt,
        FlightStatus status) implements OperationalEvent {

    public StatusEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(status, "status");
    }
}
