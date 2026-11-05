package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Trip(
        String bookingId,
        List<Passenger> passengers,
        List<Segment> segments,
        DataCompleteness completeness,
        List<String> missingFields,
        Instant retrievedAt) {

    public Trip {
        Objects.requireNonNull(bookingId, "bookingId");
        Objects.requireNonNull(completeness, "completeness");
        Objects.requireNonNull(retrievedAt, "retrievedAt");
        passengers = List.copyOf(passengers);
        segments = List.copyOf(segments);
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
        if (passengers.isEmpty()) {
            throw new IllegalArgumentException("A trip must have at least one passenger");
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("A trip must have at least one segment");
        }
    }

    public int passengerCount() {
        return passengers.size();
    }

    public boolean groupIncludesMinor() {
        return passengers.stream().anyMatch(Passenger::isMinor);
    }

    public List<String> segmentIds() {
        return segments.stream().map(Segment::segmentId).toList();
    }

    public Optional<Segment> segment(String segmentId) {
        return segments.stream().filter(s -> s.segmentId().equals(segmentId)).findFirst();
    }

    public String finalDestination() {
        return segments.get(segments.size() - 1).destination();
    }

    public boolean isPartial() {
        return completeness == DataCompleteness.PARTIAL;
    }
}
