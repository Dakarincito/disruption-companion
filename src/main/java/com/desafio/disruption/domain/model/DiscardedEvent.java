package com.desafio.disruption.domain.model;

import java.util.Objects;

public record DiscardedEvent(String eventId, String source, DiscardReason reason) {

    public DiscardedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reason, "reason");
    }
}
