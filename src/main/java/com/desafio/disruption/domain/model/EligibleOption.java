package com.desafio.disruption.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public record EligibleOption(
        String optionId,
        String type,
        String flightNumber,
        String origin,
        String destination,
        LocalDate date,
        LocalTime departure,
        BigDecimal fareDifference,
        String currency,
        Integer seatsAvailable,
        String policyRef,
        Confidence confidence) {

    public static final String REBOOK = "REBOOK";
    public static final String REFUND = "REFUND";

    public EligibleOption {
        Objects.requireNonNull(optionId, "optionId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(confidence, "confidence");
    }

    public Optional<LocalDateTime> departureAt() {
        return date == null || departure == null ? Optional.empty() : Optional.of(LocalDateTime.of(date, departure));
    }

    public boolean hasFareDifference() {
        return fareDifference != null && fareDifference.signum() > 0;
    }

    public enum Confidence {
        HIGH,
        MEDIUM,
        LOW
    }
}
