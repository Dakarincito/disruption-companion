package com.desafio.disruption.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DisruptionAssessment(
        String bookingId,
        Instant assessedAt,
        Disruption disruption,
        EventResolution events,
        List<ConnectionImpact> connections,
        PolicyEvaluation policy,
        OptionEvaluation options,
        Decision decision,
        List<ReviewReason> reviewReasons) {

    public DisruptionAssessment {
        Objects.requireNonNull(bookingId, "bookingId");
        Objects.requireNonNull(assessedAt, "assessedAt");
        Objects.requireNonNull(disruption, "disruption");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(decision, "decision");
        connections = List.copyOf(connections);
        reviewReasons = List.copyOf(reviewReasons);
        if (decision == Decision.REQUIRES_HUMAN && reviewReasons.isEmpty()) {
            throw new IllegalArgumentException("REQUIRES_HUMAN must state at least one reason");
        }
    }

    public Optional<PolicyEvaluation> policyEvaluation() {
        return Optional.ofNullable(policy);
    }

    public Optional<OptionEvaluation> optionEvaluation() {
        return Optional.ofNullable(options);
    }
}
