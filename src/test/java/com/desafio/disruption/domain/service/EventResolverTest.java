package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.DataCompleteness;
import com.desafio.disruption.domain.model.DelayEvent;
import com.desafio.disruption.domain.model.DiscardReason;
import com.desafio.disruption.domain.model.DiscardedEvent;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.FlightStatus;
import com.desafio.disruption.domain.model.GateChangeEvent;
import com.desafio.disruption.domain.model.OperationalEvent;
import com.desafio.disruption.domain.model.OperationalSnapshot;
import com.desafio.disruption.domain.model.Passenger;
import com.desafio.disruption.domain.model.PassengerType;
import com.desafio.disruption.domain.model.Segment;
import com.desafio.disruption.domain.model.SegmentStatus;
import com.desafio.disruption.domain.model.StatusEvent;
import com.desafio.disruption.domain.model.Trip;
import com.desafio.disruption.domain.model.UnknownEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventResolverTest {

    private static final Instant NOW = Instant.parse("2026-09-15T11:03:00Z");
    private static final LocalDate DAY = LocalDate.parse("2026-09-15");

    private final EventResolver resolver = new EventResolver(Duration.ofMinutes(60));

    private static Trip trip() {
        return new Trip("ABC123",
                List.of(new Passenger("P1", PassengerType.ADULT, null), new Passenger("P2", PassengerType.CHILD, 7)),
                List.of(new Segment("S1", "LA500", "SCL", "LIM", DAY, LocalTime.parse("14:30"), LocalTime.parse("17:55")),
                        new Segment("S2", "LA2410", "LIM", "MEX", DAY, LocalTime.parse("19:05"), LocalTime.parse("23:40"))),
                DataCompleteness.PARTIAL,
                List.of("fare_conditions.change_penalty_amount"),
                Instant.parse("2026-09-15T11:02:31Z"));
    }

    private static DelayEvent delayS1() {
        return new DelayEvent("E-1001", "S1", "OPS_CORE", Instant.parse("2026-09-15T10:48:00Z"), LocalTime.parse("17:10"), 160);
    }

    private static GateChangeEvent gateS1() {
        return new GateChangeEvent("E-1007", "S1", "AIRPORT_FEED", Instant.parse("2026-09-15T10:55:00Z"), "24");
    }

    private static StatusEvent legacyOnTimeS1() {
        return new StatusEvent("E-1012", "S1", "LEGACY_MIRROR", Instant.parse("2026-09-15T09:30:00Z"), FlightStatus.ON_TIME);
    }

    private static StatusEvent scheduledS2() {
        return new StatusEvent("E-2003", "S2", "OPS_CORE", Instant.parse("2026-09-15T10:48:00Z"), FlightStatus.SCHEDULED);
    }

    private static OperationalSnapshot snapshot(List<OperationalEvent> events) {
        return new OperationalSnapshot(events, Set.of("LEGACY_MIRROR"), Instant.parse("2026-09-15T11:02:33Z"));
    }

    private static StatusEvent trustedOnTimeS1(String recordedAt) {
        return new StatusEvent("E-9", "S1", "OPS_CORE", Instant.parse(recordedAt), FlightStatus.ON_TIME);
    }

    @Test
    void baseCase_resolvesDelayAndGateAndDiscardsDegradedSource() {
        EventResolution result = resolver.resolve(trip(),
                snapshot(List.of(delayS1(), gateS1(), legacyOnTimeS1(), scheduledS2())), NOW);

        SegmentStatus s1 = result.status("S1");
        assertEquals(160, s1.delayMinutes());
        assertEquals(LocalTime.parse("17:10"), s1.estimatedDeparture());
        assertEquals("24", s1.gate());
        assertEquals(List.of("E-1001", "E-1007"), s1.evidenceEventIds());

        SegmentStatus s2 = result.status("S2");
        assertEquals(0, s2.delayMinutes());
        assertEquals(LocalTime.parse("19:05"), s2.estimatedDeparture());

        assertEquals(List.of(new DiscardedEvent("E-1012", "LEGACY_MIRROR", DiscardReason.SOURCE_DEGRADED)),
                result.discardedEvents());
        assertTrue(result.isReliable());
    }

    @Test
    void trustedOnTimeAfterDelay_isARealConflict() {
        EventResolution result = resolver.resolve(trip(),
                snapshot(List.of(delayS1(), trustedOnTimeS1("2026-09-15T10:58:00Z"))), NOW);

        assertEquals(List.of("S1"), result.conflictingSegments());
        assertFalse(result.isReliable());
    }

    @Test
    void trustedOnTimeBeforeDelay_isSupersededNotAConflict() {
        EventResolution result = resolver.resolve(trip(),
                snapshot(List.of(delayS1(), trustedOnTimeS1("2026-09-15T09:00:00Z"))), NOW);

        assertTrue(result.conflictingSegments().isEmpty());
        assertEquals(DiscardReason.SUPERSEDED_BY_NEWER_EVENT, result.discardedEvents().get(0).reason());
    }

    @Test
    void delayMinutesNotMatchingNewDeparture_isAConflict() {
        DelayEvent inconsistent = new DelayEvent("E-1001", "S1", "OPS_CORE",
                Instant.parse("2026-09-15T10:48:00Z"), LocalTime.parse("17:10"), 90);

        EventResolution result = resolver.resolve(trip(), snapshot(List.of(inconsistent)), NOW);

        assertEquals(List.of("S1"), result.conflictingSegments());
    }

    @Test
    void delayOlderThanMaxAge_isStale() {
        EventResolution result = resolver.resolve(trip(), snapshot(List.of(delayS1())),
                Instant.parse("2026-09-15T12:00:00Z"));

        assertEquals(List.of("S1"), result.staleSegments());
        assertFalse(result.isReliable());
    }

    @Test
    void unknownEventType_isDiscardedWithReason() {
        UnknownEvent swap = new UnknownEvent("E-7", "S1", "OPS_CORE", Instant.parse("2026-09-15T10:50:00Z"), "AIRCRAFT_SWAP");

        EventResolution result = resolver.resolve(trip(), snapshot(List.of(swap)), NOW);

        assertEquals(DiscardReason.UNSUPPORTED_EVENT_TYPE, result.discardedEvents().get(0).reason());
        assertEquals(0, result.status("S1").delayMinutes());
        assertNull(result.status("S1").gate());
    }
}
