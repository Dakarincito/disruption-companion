package com.desafio.disruption.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EventResolution(
        Map<String, SegmentStatus> statusBySegment,
        List<DiscardedEvent> discardedEvents,
        List<String> conflictingSegments,
        List<String> staleSegments) {

    public EventResolution {
        statusBySegment = Collections.unmodifiableMap(new LinkedHashMap<>(statusBySegment));
        discardedEvents = List.copyOf(discardedEvents);
        conflictingSegments = conflictingSegments.stream().distinct().toList();
        staleSegments = staleSegments.stream().distinct().toList();
    }

    public SegmentStatus status(String segmentId) {
        SegmentStatus status = statusBySegment.get(segmentId);
        if (status == null) {
            throw new IllegalArgumentException("Unknown segment: " + segmentId);
        }
        return status;
    }

    public int maxDelayMinutes() {
        return statusBySegment.values().stream().mapToInt(SegmentStatus::delayMinutes).max().orElse(0);
    }

    public boolean isReliable() {
        return conflictingSegments.isEmpty() && staleSegments.isEmpty();
    }
}
