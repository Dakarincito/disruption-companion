package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.DataCompleteness;
import com.desafio.disruption.domain.model.Decision;
import com.desafio.disruption.domain.model.DelayEvent;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.EligibleOption;
import com.desafio.disruption.domain.model.EligibleOption.Confidence;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.OperationalSnapshot;
import com.desafio.disruption.domain.model.OptionEvaluation;
import com.desafio.disruption.domain.model.OptionEvaluation.Issue;
import com.desafio.disruption.domain.model.OptionSet;
import com.desafio.disruption.domain.model.Passenger;
import com.desafio.disruption.domain.model.PassengerType;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.PolicySet;
import com.desafio.disruption.domain.model.ReviewReason;
import com.desafio.disruption.domain.model.Segment;
import com.desafio.disruption.domain.model.Trip;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionFlowTest {

    private static final Instant NOW = Instant.parse("2026-09-15T11:03:00Z");
    private static final LocalDate DAY = LocalDate.parse("2026-09-15");
    private static final Duration MCT = Duration.ofMinutes(60);

    private static final String POLICY = "POL-DELAY-2026-03";

    @Test
    void baseCase_requiresHumanBecauseOfTheMinor() {
        Result result = run(tripWithMinor(), delayPolicy(), baseOptions());

        assertEquals(List.of("R3", "R5"), result.policy().appliedRules().stream().map(PolicySet.Rule::ruleId).toList());
        assertEquals("O1", result.options().candidate().optionId());
        assertEquals(List.of(Issue.INSUFFICIENT_SEATS, Issue.FARE_CONFLICTS_WITH_ENTITLEMENT), reasonsFor(result, "O2"));
        assertEquals(List.of(Issue.POLICY_NOT_VERIFIED, Issue.LOW_CONFIDENCE), reasonsFor(result, "O3"));

        assertEquals(Decision.REQUIRES_HUMAN, result.outcome().decision());
        assertEquals(List.of(new ReviewReason(ReviewReason.Code.POLICY_REQUIRES_APPROVAL, "R5 PRIORITY_HANDLING")),
                result.outcome().reasons());
    }

    @Test
    void adultTravellingAloneWithAValidOption_isRecommendedAutomatically() {
        Trip adultOnly = trip(List.of(new Passenger("P1", PassengerType.ADULT, null)));

        Result result = run(adultOnly, delayPolicy(), baseOptions());

        assertEquals(Decision.RECOMMEND_TO_PASSENGER, result.outcome().decision());
        assertTrue(result.outcome().reasons().isEmpty());
    }

    @Test
    void unknownPolicyCondition_isNeverGuessedAndGoesToHuman() {
        PolicySet withNewRule = new PolicySet("DELAY", POLICY, LocalDate.parse("2026-03-01"),
                List.of(new PolicySet.Rule("R3", "missed_connection == true", "REBOOK_NO_FEE", false),
                        new PolicySet.Rule("R9", "loyalty_tier == GOLD", "LOUNGE_ACCESS", false)),
                Set.of());
        Trip adultOnly = trip(List.of(new Passenger("P1", PassengerType.ADULT, null)));

        Result result = run(adultOnly, withNewRule, baseOptions());

        assertEquals(List.of("R9"), result.policy().undeterminedRuleIds());
        assertEquals(Decision.REQUIRES_HUMAN, result.outcome().decision());
    }

    private record Result(PolicyEvaluation policy, OptionEvaluation options, DecisionMaker.Outcome outcome) {
    }

    private static Result run(Trip trip, PolicySet policySet, OptionSet optionSet) {
        DelayEvent delay = new DelayEvent("E-1001", "S1", "OPS_CORE",
                Instant.parse("2026-09-15T10:48:00Z"), LocalTime.parse("17:10"), 160);
        EventResolution events = new EventResolver(Duration.ofMinutes(60))
                .resolve(trip, new OperationalSnapshot(List.of(delay), Set.of(), NOW), NOW);
        List<ConnectionImpact> connections = new ConnectionAnalyzer(MCT).analyze(trip, events);
        Disruption disruption = new DisruptionClassifier(15).classify(events, connections);

        PolicyEvaluation policy = new PolicyEvaluator().evaluate(policySet, trip, events, connections);
        OptionEvaluation options = new OptionValidator(MCT).validate(optionSet, trip, disruption, policySet, policy, NOW);
        DecisionMaker.Outcome outcome = new DecisionMaker().decide(trip, disruption, events, policy, options);
        return new Result(policy, options, outcome);
    }

    private static List<Issue> reasonsFor(Result result, String optionId) {
        return result.options().rejected().stream()
                .filter(r -> r.optionId().equals(optionId))
                .findFirst()
                .orElseThrow()
                .reasons();
    }

    private static Trip tripWithMinor() {
        return trip(List.of(new Passenger("P1", PassengerType.ADULT, null), new Passenger("P2", PassengerType.CHILD, 7)));
    }

    private static Trip trip(List<Passenger> passengers) {
        return new Trip("ABC123", passengers,
                List.of(new Segment("S1", "LA500", "SCL", "LIM", DAY, LocalTime.parse("14:30"), LocalTime.parse("17:55")),
                        new Segment("S2", "LA2410", "LIM", "MEX", DAY, LocalTime.parse("19:05"), LocalTime.parse("23:40"))),
                DataCompleteness.PARTIAL, List.of("fare_conditions.change_penalty_amount"),
                Instant.parse("2026-09-15T11:02:31Z"));
    }

    private static PolicySet delayPolicy() {
        return new PolicySet("DELAY", POLICY, LocalDate.parse("2026-03-01"), List.of(
                new PolicySet.Rule("R1", "delay_minutes >= 180", "REBOOK_NO_FEE", false),
                new PolicySet.Rule("R2", "delay_minutes >= 240 AND international", "MEAL_VOUCHER", false),
                new PolicySet.Rule("R3", "missed_connection == true", "REBOOK_NO_FEE", false),
                new PolicySet.Rule("R4", "compensation_requested == true", "CASH_COMPENSATION", true),
                new PolicySet.Rule("R5", "group_includes_minor == true", "PRIORITY_HANDLING", true)),
                Set.of("POL-DELAY-2025-11"));
    }

    private static OptionSet baseOptions() {
        return new OptionSet(List.of(
                new EligibleOption("O1", "REBOOK", "LA2412", "LIM", "MEX", LocalDate.parse("2026-09-16"),
                        LocalTime.parse("08:15"), BigDecimal.ZERO, null, 4, POLICY, Confidence.HIGH),
                new EligibleOption("O2", "REBOOK", "LA2450", "LIM", "MEX", DAY,
                        LocalTime.parse("23:40"), new BigDecimal("180000"), "CLP", 1, POLICY, Confidence.MEDIUM),
                new EligibleOption("O3", "REFUND", null, null, null, null, null, null, null, null,
                        "POL-REFUND-2026-01", Confidence.LOW)),
                Instant.parse("2026-09-15T11:02:35Z"), Duration.ofSeconds(120));
    }
}
