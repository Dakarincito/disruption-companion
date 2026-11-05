package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

public record DelayEvent(
        String eventId,
        String segmentId,
        String source,
        Instant recordedAt,
        LocalTime newDeparture,
        int delayMinutes) implements OperationalEvent {

    public DelayEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(newDeparture, "newDeparture");
        if (delayMinutes < 0) {
            throw new IllegalArgumentException("delayMinutes must not be negative");
        }
    }
}
