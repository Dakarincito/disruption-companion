package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.util.Objects;

public record GateChangeEvent(
        String eventId,
        String segmentId,
        String source,
        Instant recordedAt,
        String gate) implements OperationalEvent {

    public GateChangeEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(gate, "gate");
    }
}
