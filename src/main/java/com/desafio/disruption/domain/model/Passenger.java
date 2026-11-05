package com.desafio.disruption.domain.model;

import java.util.Objects;

public record Passenger(String passengerId, PassengerType type, Integer age) {

    private static final int ADULT_AGE = 18;

    public Passenger {
        Objects.requireNonNull(passengerId, "passengerId");
        Objects.requireNonNull(type, "type");
    }

    public boolean isMinor() {
        return type != PassengerType.ADULT || (age != null && age < ADULT_AGE);
    }
}
