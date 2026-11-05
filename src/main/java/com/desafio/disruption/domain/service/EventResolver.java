package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.DelayEvent;
import com.desafio.disruption.domain.model.DiscardReason;
import com.desafio.disruption.domain.model.DiscardedEvent;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.FlightStatus;
import com.desafio.disruption.domain.model.GateChangeEvent;
import com.desafio.disruption.domain.model.OperationalEvent;
import com.desafio.disruption.domain.model.OperationalSnapshot;
import com.desafio.disruption.domain.model.Segment;
import com.desafio.disruption.domain.model.SegmentStatus;
import com.desafio.disruption.domain.model.StatusEvent;
import com.desafio.disruption.domain.model.Trip;
import com.desafio.disruption.domain.model.UnknownEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


//resolver eventos
public final class EventResolver {

    private final Duration maxEventAge;

    public EventResolver(Duration maxEventAge) {
        this.maxEventAge = Objects.requireNonNull(maxEventAge, "maxEventAge");
    }

    public EventResolution resolve(Trip trip, OperationalSnapshot snapshot, Instant now) {
        List<DiscardedEvent> discarded = new ArrayList<>();
        Map<String, List<OperationalEvent>> acceptedBySegment = new HashMap<>();

        for (OperationalEvent event : snapshot.events()) {
            if (snapshot.degradedSources().contains(event.source())) {
                discarded.add(discard(event, DiscardReason.SOURCE_DEGRADED));
            } else if (event instanceof UnknownEvent) {
                discarded.add(discard(event, DiscardReason.UNSUPPORTED_EVENT_TYPE));
            } else {
                acceptedBySegment.computeIfAbsent(event.segmentId(), id -> new ArrayList<>()).add(event);
            }
        }

        Map<String, SegmentStatus> statuses = new LinkedHashMap<>();
        List<String> conflicting = new ArrayList<>();
        List<String> stale = new ArrayList<>();

        for (Segment segment : trip.segments()) {
            List<OperationalEvent> events = acceptedBySegment.getOrDefault(segment.segmentId(), List.of());
            statuses.put(segment.segmentId(), resolveSegment(segment, events, now, discarded, conflicting, stale));
        }
        return new EventResolution(statuses, discarded, conflicting, stale);
    }

    private SegmentStatus resolveSegment(Segment segment,
                                         List<OperationalEvent> events,
                                         Instant now,
                                         List<DiscardedEvent> discarded,
                                         List<String> conflicting,
                                         List<String> stale) {
        Optional<DelayEvent> delay = latest(events, DelayEvent.class);
        Optional<StatusEvent> status = latest(events, StatusEvent.class);
        Optional<GateChangeEvent> gate = latest(events, GateChangeEvent.class);

        List<String> evidence = new ArrayList<>();
        int delayMinutes = 0;
        LocalTime estimatedDeparture = segment.scheduledDeparture();

        if (delay.isPresent()) {
            DelayEvent delayEvent = delay.get();
            evidence.add(delayEvent.eventId());
            delayMinutes = delayEvent.delayMinutes();
            estimatedDeparture = delayEvent.newDeparture();

            if (!isConsistent(segment, delayEvent)) {
                conflicting.add(segment.segmentId());
            }
            if (status.isPresent() && status.get().status() == FlightStatus.ON_TIME) {
                StatusEvent onTime = status.get();
                if (onTime.recordedAt().isAfter(delayEvent.recordedAt())) {
                    conflicting.add(segment.segmentId());
                } else {
                    discarded.add(discard(onTime, DiscardReason.SUPERSEDED_BY_NEWER_EVENT));
                }
            }
            if (Duration.between(delayEvent.recordedAt(), now).compareTo(maxEventAge) > 0) {
                stale.add(segment.segmentId());
            }
        }
        gate.ifPresent(g -> evidence.add(g.eventId()));

        return new SegmentStatus(
                segment.segmentId(),
                delayMinutes,
                estimatedDeparture,
                gate.map(GateChangeEvent::gate).orElse(null),
                evidence);
    }

    private static <T extends OperationalEvent> Optional<T> latest(List<OperationalEvent> events, Class<T> type) {
        return events.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .max(Comparator.comparing(OperationalEvent::recordedAt));
    }

    private static boolean isConsistent(Segment segment, DelayEvent delay) {
        LocalDateTime scheduled = segment.scheduledDepartureAt();
        LocalDateTime estimated = LocalDateTime.of(segment.date(), delay.newDeparture());
        if (estimated.isBefore(scheduled)) {
            estimated = estimated.plusDays(1);
        }
        return Duration.between(scheduled, estimated).toMinutes() == delay.delayMinutes();
    }

    private static DiscardedEvent discard(OperationalEvent event, DiscardReason reason) {
        return new DiscardedEvent(event.eventId(), event.source(), reason);
    }
}
