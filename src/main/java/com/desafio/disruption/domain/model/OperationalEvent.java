package com.desafio.disruption.domain.model;

import java.time.Instant;

public sealed interface OperationalEvent
        permits DelayEvent, GateChangeEvent, StatusEvent, UnknownEvent {

    String eventId();

    String segmentId();

    String source();

    Instant recordedAt();
}
