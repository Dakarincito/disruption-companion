package com.desafio.disruption.domain.model;

import java.util.List;
import java.util.Optional;

public record OptionEvaluation(EligibleOption candidate, List<Rejected> rejected) {

    public OptionEvaluation {
        rejected = List.copyOf(rejected);
    }

    public Optional<EligibleOption> bestCandidate() {
        return Optional.ofNullable(candidate);
    }

    public record Rejected(String optionId, List<Issue> reasons) {

        public Rejected {
            reasons = List.copyOf(reasons);
        }
    }

    public enum Issue {
        OPTIONS_EXPIRED,
        POLICY_SUPERSEDED,
        POLICY_NOT_VERIFIED,
        UNSUPPORTED_OPTION_TYPE,
        INSUFFICIENT_SEATS,
        ROUTE_MISMATCH,
        NOT_REACHABLE,
        FARE_CONFLICTS_WITH_ENTITLEMENT,
        LOW_CONFIDENCE
    }
}
