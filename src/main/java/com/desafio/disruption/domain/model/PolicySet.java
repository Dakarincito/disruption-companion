package com.desafio.disruption.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PolicySet(
        String scenario,
        String version,
        LocalDate effectiveFrom,
        List<Rule> rules,
        Set<String> supersededVersions) {

    public PolicySet {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        rules = List.copyOf(rules);
        supersededVersions = supersededVersions == null ? Set.of() : Set.copyOf(supersededVersions);
    }

    public boolean isEffectiveOn(LocalDate date) {
        return !effectiveFrom.isAfter(date);
    }

    public record Rule(String ruleId, String condition, String entitlement, boolean requiresHumanApproval) {

        public Rule {
            Objects.requireNonNull(ruleId, "ruleId");
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(entitlement, "entitlement");
        }
    }
}
