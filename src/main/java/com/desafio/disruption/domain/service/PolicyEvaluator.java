package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.PolicySet;
import com.desafio.disruption.domain.model.Trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//para evaluar politicas de la tool de policy
public final class PolicyEvaluator {

    public PolicyEvaluation evaluate(PolicySet policy, Trip trip, EventResolution events,
                                     List<ConnectionImpact> connections) {
        Facts facts = new Facts(
                events.maxDelayMinutes(),
                connections.stream().anyMatch(ConnectionImpact::missed),
                trip.groupIncludesMinor());

        List<PolicySet.Rule> applied = new ArrayList<>();
        List<String> undetermined = new ArrayList<>();
        for (PolicySet.Rule rule : policy.rules()) {
            Optional<Boolean> result = evaluate(rule.condition(), facts);
            if (result.isEmpty()) {
                undetermined.add(rule.ruleId());
            } else if (result.get()) {
                applied.add(rule);
            }
        }
        boolean effective = policy.isEffectiveOn(trip.segments().get(0).date());
        return new PolicyEvaluation(policy.version(), effective, applied, undetermined);
    }

    private static Optional<Boolean> evaluate(String condition, Facts facts) {
        return switch (condition.trim()) {
            case "delay_minutes >= 180" -> Optional.of(facts.delayMinutes() >= 180);
            // el contrato no trae países solo se puede concluir si el retraso es < 240 segun vi en el pdf
            case "delay_minutes >= 240 AND international" ->
                    facts.delayMinutes() < 240 ? Optional.of(false) : Optional.empty();
            case "missed_connection == true" -> Optional.of(facts.missedConnection());
            // la evaluación no sale de una solicitud del pasajero
            case "compensation_requested == true" -> Optional.of(false);
            case "group_includes_minor == true" -> Optional.of(facts.groupIncludesMinor());
            default -> Optional.empty();
        };
    }

    private record Facts(int delayMinutes, boolean missedConnection, boolean groupIncludesMinor) {
    }
}
