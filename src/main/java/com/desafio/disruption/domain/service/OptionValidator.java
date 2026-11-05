package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.EligibleOption;
import com.desafio.disruption.domain.model.OptionEvaluation;
import com.desafio.disruption.domain.model.OptionEvaluation.Issue;
import com.desafio.disruption.domain.model.OptionSet;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.PolicySet;
import com.desafio.disruption.domain.model.Trip;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


//para validar opciones
public final class OptionValidator {

    private static final Comparator<EligibleOption> PRIORITY = Comparator
            .comparing(EligibleOption::confidence)
            .thenComparing(option -> option.departureAt().orElse(LocalDateTime.MAX));

    private final Duration minimumConnectionTime;

    public OptionValidator(Duration minimumConnectionTime) {
        this.minimumConnectionTime = Objects.requireNonNull(minimumConnectionTime, "minimumConnectionTime");
    }

    public OptionEvaluation validate(OptionSet optionSet, Trip trip, Disruption disruption,
                                     PolicySet policy, PolicyEvaluation policyEvaluation, Instant now) {
        boolean expired = optionSet.isExpiredAt(now);
        List<EligibleOption> valid = new ArrayList<>();
        List<OptionEvaluation.Rejected> rejected = new ArrayList<>();

        for (EligibleOption option : optionSet.options()) {
            Set<Issue> issues = issuesOf(option, trip, disruption, policy, policyEvaluation, expired);
            if (issues.isEmpty()) {
                valid.add(option);
            } else {
                rejected.add(new OptionEvaluation.Rejected(option.optionId(), List.copyOf(issues)));
            }
        }
        EligibleOption best = valid.stream().min(PRIORITY).orElse(null);
        return new OptionEvaluation(best, rejected);
    }

    private Set<Issue> issuesOf(EligibleOption option, Trip trip, Disruption disruption,
                                PolicySet policy, PolicyEvaluation policyEvaluation, boolean expired) {
        Set<Issue> issues = EnumSet.noneOf(Issue.class);

        if (expired) {
            issues.add(Issue.OPTIONS_EXPIRED);
        }
        if (policy.supersededVersions().contains(option.policyRef())) {
            issues.add(Issue.POLICY_SUPERSEDED);
        } else if (!policy.version().equals(option.policyRef())) {
            issues.add(Issue.POLICY_NOT_VERIFIED);
        }

        if (EligibleOption.REBOOK.equals(option.type())) {
            addRebookIssues(option, trip, disruption.affectedConnection(), policyEvaluation, issues);
        } else if (!EligibleOption.REFUND.equals(option.type())) {
            issues.add(Issue.UNSUPPORTED_OPTION_TYPE);
        }

        if (option.confidence() == EligibleOption.Confidence.LOW) {
            issues.add(Issue.LOW_CONFIDENCE);
        }
        return issues;
    }

    private void addRebookIssues(EligibleOption option, Trip trip, Optional<ConnectionImpact> connection,
                                 PolicyEvaluation policyEvaluation, Set<Issue> issues) {
        // El grupo viaja junto.
        if (option.seatsAvailable() == null || option.seatsAvailable() < trip.passengerCount()) {
            issues.add(Issue.INSUFFICIENT_SEATS);
        }
        if (option.hasFareDifference() && policyEvaluation.grants(PolicyEvaluation.REBOOK_NO_FEE)) {
            issues.add(Issue.FARE_CONFLICTS_WITH_ENTITLEMENT);
        }
        if (connection.isPresent()) {
            ConnectionImpact missed = connection.get();
            boolean sameRoute = missed.connectionAirport().equals(option.origin())
                    && trip.finalDestination().equals(option.destination());
            if (!sameRoute) {
                issues.add(Issue.ROUTE_MISMATCH);
            }
            LocalDateTime readyFrom = missed.projectedArrival().plus(minimumConnectionTime);
            if (option.departureAt().map(departure -> departure.isBefore(readyFrom)).orElse(true)) {
                issues.add(Issue.NOT_REACHABLE);
            }
        }
    }
}
