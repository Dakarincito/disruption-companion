package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record OperationalSnapshot(List<OperationalEvent> events, Set<String> degradedSources, Instant retrievedAt) {

    public OperationalSnapshot {
        Objects.requireNonNull(retrievedAt, "retrievedAt");
        events = events == null ? List.of() : List.copyOf(events);
        degradedSources = degradedSources == null ? Set.of() : Set.copyOf(degradedSources);
    }
}
