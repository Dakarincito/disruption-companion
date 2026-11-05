package com.desafio.disruption.domain.model;

import java.util.Objects;
import java.util.Optional;

public record Disruption(DisruptionType type, String segmentId, int delayMinutes, ConnectionImpact connection) {

    public Disruption {
        Objects.requireNonNull(type, "type");
        if (type != DisruptionType.NONE) {
            Objects.requireNonNull(segmentId, "segmentId");
        }
        if (type == DisruptionType.MISSED_CONNECTION) {
            Objects.requireNonNull(connection, "connection");
        }
    }

    public static Disruption none() {
        return new Disruption(DisruptionType.NONE, null, 0, null);
    }

    public boolean isRelevant() {
        return type.isRelevant();
    }

    public Optional<ConnectionImpact> affectedConnection() {
        return Optional.ofNullable(connection);
    }
}
