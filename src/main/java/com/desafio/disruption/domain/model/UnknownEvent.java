package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.util.Objects;

public record UnknownEvent(
        String eventId,
        String segmentId,
        String source,
        Instant recordedAt,
        String rawType) implements OperationalEvent {

    public UnknownEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }
}
