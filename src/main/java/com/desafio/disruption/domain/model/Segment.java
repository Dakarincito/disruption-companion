package com.desafio.disruption.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public record Segment(
        String segmentId,
        String flightNumber,
        String origin,
        String destination,
        LocalDate date,
        LocalTime scheduledDeparture,
        LocalTime scheduledArrival) {

    public Segment {
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(flightNumber, "flightNumber");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(scheduledDeparture, "scheduledDeparture");
        Objects.requireNonNull(scheduledArrival, "scheduledArrival");
    }

    public LocalDateTime scheduledDepartureAt() {
        return LocalDateTime.of(date, scheduledDeparture);
    }

    public LocalDateTime scheduledArrivalAt() {
        // Si llega antes de la hora de salida, llega al día siguiente.
        LocalDateTime arrival = LocalDateTime.of(date, scheduledArrival);
        return scheduledArrival.isBefore(scheduledDeparture) ? arrival.plusDays(1) : arrival;
    }
}
