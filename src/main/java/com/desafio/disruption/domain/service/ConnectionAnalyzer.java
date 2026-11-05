package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.Segment;
import com.desafio.disruption.domain.model.Trip;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//aca voy a analizar las conexiones y ver si hay diferencias entre llegada y salida y posibles conflictos
public final class ConnectionAnalyzer {

    private final Duration minimumConnectionTime;

    public ConnectionAnalyzer(Duration minimumConnectionTime) {
        this.minimumConnectionTime = Objects.requireNonNull(minimumConnectionTime, "minimumConnectionTime");
    }

    public List<ConnectionImpact> analyze(Trip trip, EventResolution events) {
        List<Segment> segments = trip.segments();
        List<ConnectionImpact> impacts = new ArrayList<>();

        for (int i = 0; i < segments.size() - 1; i++) {
            Segment arriving = segments.get(i);
            Segment departing = segments.get(i + 1);
            if (isConnection(arriving, departing)) {
                impacts.add(impactOf(arriving, departing, events));
            }
        }
        return List.copyOf(impacts);
    }

    private static boolean isConnection(Segment arriving, Segment departing) {
        return arriving.destination().equals(departing.origin());
    }

    private ConnectionImpact impactOf(Segment arriving, Segment departing, EventResolution events) {
        LocalDateTime projectedArrival = arriving.scheduledArrivalAt()
                .plusMinutes(events.status(arriving.segmentId()).delayMinutes());
        LocalDateTime connectionDeparture = departing.scheduledDepartureAt()
                .plusMinutes(events.status(departing.segmentId()).delayMinutes());
        Duration available = Duration.between(projectedArrival, connectionDeparture);

        return new ConnectionImpact(
                arriving.segmentId(),
                departing.segmentId(),
                arriving.destination(),
                projectedArrival,
                connectionDeparture,
                available,
                available.compareTo(minimumConnectionTime) < 0);
    }
}
