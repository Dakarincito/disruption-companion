package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.Decision;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.DisruptionType;
import com.desafio.disruption.domain.model.EligibleOption;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.OptionEvaluation;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.PolicySet;
import com.desafio.disruption.domain.model.ReviewReason;
import com.desafio.disruption.domain.model.ReviewReason.Code;
import com.desafio.disruption.domain.model.Trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//clase para la toma de decicion si el pasajero puede ser compensado o no y si hay opciones disponibles
//para mandar dsps a la Notice
public final class DecisionMaker {

    public record Outcome(Decision decision, List<ReviewReason> reasons) {

        public Outcome {
            reasons = List.copyOf(reasons);
        }
    }

    public Outcome decide(Trip trip, Disruption disruption, EventResolution events,
                          PolicyEvaluation policy, OptionEvaluation options) {
        List<ReviewReason> reasons = new ArrayList<>();
        events.conflictingSegments().forEach(id -> reasons.add(new ReviewReason(Code.SOURCE_CONFLICT, id)));
        events.staleSegments().forEach(id -> reasons.add(new ReviewReason(Code.STALE_OPERATIONAL_DATA, id)));

        if (!disruption.isRelevant()) {
            if (!reasons.isEmpty()) {
                return new Outcome(Decision.REQUIRES_HUMAN, reasons);
            }
            Decision decision = disruption.type() == DisruptionType.GATE_CHANGE
                    ? Decision.INFORM_PASSENGER
                    : Decision.NO_ACTION;
            return new Outcome(decision, reasons);
        }

        if (!policy.effective()) {
            reasons.add(new ReviewReason(Code.POLICY_NOT_EFFECTIVE, policy.policyVersion()));
        }
        policy.undeterminedRuleIds().forEach(id -> reasons.add(new ReviewReason(Code.POLICY_RULE_UNDETERMINED, id)));
        for (PolicySet.Rule rule : policy.rulesRequiringApproval()) {
            reasons.add(new ReviewReason(Code.POLICY_REQUIRES_APPROVAL, rule.ruleId() + " " + rule.entitlement()));
        }

        if (policy.appliedRules().isEmpty()) {
            return reasons.isEmpty()
                    ? new Outcome(Decision.INFORM_PASSENGER, reasons)
                    : new Outcome(Decision.REQUIRES_HUMAN, reasons);
        }

        Optional<EligibleOption> candidate = options.bestCandidate();
        if (candidate.isEmpty()) {
            reasons.add(new ReviewReason(Code.NO_VALID_OPTION, null));
        } else if (candidate.get().confidence() != EligibleOption.Confidence.HIGH) {
            reasons.add(new ReviewReason(Code.CANDIDATE_NOT_HIGH_CONFIDENCE, candidate.get().optionId()));
        }
        if (trip.isPartial() && !policy.grants(PolicyEvaluation.REBOOK_NO_FEE)) {
            reasons.add(new ReviewReason(Code.INCOMPLETE_TRIP_DATA, String.join(", ", trip.missingFields())));
        }

        return reasons.isEmpty()
                ? new Outcome(Decision.RECOMMEND_TO_PASSENGER, reasons)
                : new Outcome(Decision.REQUIRES_HUMAN, reasons);
    }
}
