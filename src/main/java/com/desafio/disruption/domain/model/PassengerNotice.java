package com.desafio.disruption.domain.model;

import java.util.List;
import java.util.Objects;

public record PassengerNotice(
        Status status,
        String title,
        String message,
        List<String> details,
        NextStep nextStep,
        String policyRef) {

    public PassengerNotice {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(nextStep, "nextStep");
        details = List.copyOf(details);
    }

    public enum Status {
        NO_CHANGES,
        INFORMATION,
        DELAYED,
        CONNECTION_AFFECTED,
        UNDER_VERIFICATION
    }

    public enum NextStep {
        NONE,
        CHECK_FOR_UPDATES,
        CONFIRM_OPTION,
        AWAIT_AGENT
    }
}
