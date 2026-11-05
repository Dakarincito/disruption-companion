package com.desafio.disruption.domain.model;

public enum DisruptionType {
    NONE(false),
    GATE_CHANGE(false),
    DELAY(true),
    MISSED_CONNECTION(true);

    private final boolean relevant;

    DisruptionType(boolean relevant) {
        this.relevant = relevant;
    }

    public boolean isRelevant() {
        return relevant;
    }
}
