package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.DisruptionType;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.SegmentStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// para clasificar la interrupcion y saber si es una perdida de conexion o un retraso
public final class DisruptionClassifier {

    private final int relevantDelayMinutes;

    public DisruptionClassifier(int relevantDelayMinutes) {
        if (relevantDelayMinutes <= 0) {
            throw new IllegalArgumentException("relevantDelayMinutes must be positive");
        }
        this.relevantDelayMinutes = relevantDelayMinutes;
    }

    public Disruption classify(EventResolution events, List<ConnectionImpact> connections) {
        Optional<ConnectionImpact> missed = connections.stream().filter(ConnectionImpact::missed).findFirst();
        if (missed.isPresent()) {
            ConnectionImpact connection = missed.get();
            int delay = events.status(connection.arrivingSegmentId()).delayMinutes();
            return new Disruption(DisruptionType.MISSED_CONNECTION, connection.arrivingSegmentId(), delay, connection);
        }

        Optional<SegmentStatus> relevantDelay = events.statusBySegment().values().stream()
                .filter(status -> status.delayMinutes() >= relevantDelayMinutes)
                .max(Comparator.comparingInt(SegmentStatus::delayMinutes));
        if (relevantDelay.isPresent()) {
            SegmentStatus status = relevantDelay.get();
            return new Disruption(DisruptionType.DELAY, status.segmentId(), status.delayMinutes(), null);
        }

        Optional<SegmentStatus> gateChange = events.statusBySegment().values().stream()
                .filter(status -> status.gate() != null)
                .findFirst();
        if (gateChange.isPresent()) {
            return new Disruption(DisruptionType.GATE_CHANGE, gateChange.get().segmentId(), 0, null);
        }
        return Disruption.none();
    }
}
