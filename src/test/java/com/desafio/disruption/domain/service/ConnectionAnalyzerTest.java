package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.DataCompleteness;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.Passenger;
import com.desafio.disruption.domain.model.PassengerType;
import com.desafio.disruption.domain.model.Segment;
import com.desafio.disruption.domain.model.SegmentStatus;
import com.desafio.disruption.domain.model.Trip;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionAnalyzerTest {

    private static final LocalDate DAY = LocalDate.parse("2026-09-15");

    private static final Segment SCL_LIM = new Segment("S1", "LA500", "SCL", "LIM", DAY,
            LocalTime.parse("14:30"), LocalTime.parse("17:55"));
    private static final Segment LIM_MEX = new Segment("S2", "LA2410", "LIM", "MEX", DAY,
            LocalTime.parse("19:05"), LocalTime.parse("23:40"));
    private static final Segment MEX_JFK = new Segment("S3", "AM400", "MEX", "JFK", DAY.plusDays(1),
            LocalTime.parse("01:30"), LocalTime.parse("06:50"));

    private final ConnectionAnalyzer analyzer = new ConnectionAnalyzer(Duration.ofMinutes(60));

    @Test
    void baseCase_160MinuteDelayMissesTheConnectionInLima() {
        Trip trip = trip(SCL_LIM, LIM_MEX);

        List<ConnectionImpact> impacts = analyzer.analyze(trip, delayOnS1(trip, 160));

        assertEquals(1, impacts.size());
        assertEquals("LIM", impacts.get(0).connectionAirport());
        assertEquals(-90, impacts.get(0).availableTime().toMinutes(), "arrives 20:35, connection leaves 19:05");
        assertTrue(impacts.get(0).missed());
    }

    @Test
    void singleSegmentTrip_hasNoConnectionsAndDoesNotFail() {
        Trip trip = trip(SCL_LIM);

        assertTrue(analyzer.analyze(trip, delayOnS1(trip, 160)).isEmpty());
    }

    @Test
    void threeSegmentTrip_evaluatesEveryConnection() {
        Trip trip = trip(SCL_LIM, LIM_MEX, MEX_JFK);

        List<ConnectionImpact> impacts = analyzer.analyze(trip, delayOnS1(trip, 160));

        assertEquals(2, impacts.size());
        assertTrue(impacts.get(0).missed(), "S1 -> S2 in LIM");
        assertFalse(impacts.get(1).missed(), "S2 -> S3 in MEX, evaluated on its own");
    }

    private static Trip trip(Segment... segments) {
        return new Trip("ABC123", List.of(new Passenger("P1", PassengerType.ADULT, null)), List.of(segments),
                DataCompleteness.COMPLETE, List.of(), Instant.parse("2026-09-15T11:02:31Z"));
    }

    private static EventResolution delayOnS1(Trip trip, int minutes) {
        Map<String, SegmentStatus> statuses = new LinkedHashMap<>();
        for (Segment segment : trip.segments()) {
            int delay = segment.segmentId().equals("S1") ? minutes : 0;
            statuses.put(segment.segmentId(), new SegmentStatus(segment.segmentId(), delay,
                    segment.scheduledDeparture().plusMinutes(delay), null, List.of()));
        }
        return new EventResolution(statuses, List.of(), List.of(), List.of());
    }
}
