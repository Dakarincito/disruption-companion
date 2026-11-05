package com.desafio.disruption.domain.model;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public record SegmentStatus(
        String segmentId,
        int delayMinutes,
        LocalTime estimatedDeparture,
        String gate,
        List<String> evidenceEventIds) {

    public SegmentStatus {
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(estimatedDeparture, "estimatedDeparture");
        evidenceEventIds = List.copyOf(evidenceEventIds);
    }

    public boolean isDelayed() {
        return delayMinutes > 0;
    }
}
