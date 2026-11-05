package com.desafio.disruption.domain.model;

import java.util.Objects;

public record ReviewReason(Code code, String detail) {

    public ReviewReason {
        Objects.requireNonNull(code, "code");
    }

    public enum Code {
        SOURCE_CONFLICT,
        STALE_OPERATIONAL_DATA,
        POLICY_NOT_EFFECTIVE,
        POLICY_RULE_UNDETERMINED,
        POLICY_REQUIRES_APPROVAL,
        NO_VALID_OPTION,
        CANDIDATE_NOT_HIGH_CONFIDENCE,
        INCOMPLETE_TRIP_DATA
    }
}
