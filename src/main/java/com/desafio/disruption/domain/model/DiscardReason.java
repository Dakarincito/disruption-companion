package com.desafio.disruption.domain.model;

public enum DiscardReason {
    SOURCE_DEGRADED,
    SUPERSEDED_BY_NEWER_EVENT,
    UNSUPPORTED_EVENT_TYPE
}
