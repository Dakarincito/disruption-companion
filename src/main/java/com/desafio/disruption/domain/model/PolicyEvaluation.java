package com.desafio.disruption.domain.model;

import java.util.List;
import java.util.Objects;

public record PolicyEvaluation(
        String policyVersion,
        boolean effective,
        List<PolicySet.Rule> appliedRules,
        List<String> undeterminedRuleIds) {

    public static final String REBOOK_NO_FEE = "REBOOK_NO_FEE";
    public static final String PRIORITY_HANDLING = "PRIORITY_HANDLING";

    public PolicyEvaluation {
        Objects.requireNonNull(policyVersion, "policyVersion");
        appliedRules = List.copyOf(appliedRules);
        undeterminedRuleIds = List.copyOf(undeterminedRuleIds);
    }

    public boolean grants(String entitlement) {
        return appliedRules.stream().anyMatch(rule -> rule.entitlement().equals(entitlement));
    }

    public List<PolicySet.Rule> rulesRequiringApproval() {
        return appliedRules.stream().filter(PolicySet.Rule::requiresHumanApproval).toList();
    }
}
