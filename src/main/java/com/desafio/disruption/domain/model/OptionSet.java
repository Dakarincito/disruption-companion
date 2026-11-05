package com.desafio.disruption.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OptionSet(List<EligibleOption> options, Instant retrievedAt, Duration ttl) {

    public OptionSet {
        Objects.requireNonNull(retrievedAt, "retrievedAt");
        Objects.requireNonNull(ttl, "ttl");
        options = options == null ? List.of() : List.copyOf(options);
    }

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(retrievedAt.plus(ttl));
    }
}
