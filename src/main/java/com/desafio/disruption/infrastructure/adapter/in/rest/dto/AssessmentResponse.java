package com.desafio.disruption.infrastructure.adapter.in.rest.dto;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.DisruptionAssessment;
import com.desafio.disruption.domain.model.EligibleOption;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.OptionEvaluation;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.SegmentStatus;

import java.util.List;

public record AssessmentResponse(
        String bookingId,
        String assessedAt,
        String decision,
        List<ReviewReasonView> reviewReasons,
        DisruptionView disruption,
        PolicyView policy,
        OptionView candidateOption,
        List<RejectedOptionView> rejectedOptions,
        boolean reliableData,
        List<SegmentView> segments,
        List<DiscardedEventView> discardedEvents) {

    public static AssessmentResponse from(DisruptionAssessment assessment) {
        EventResolution events = assessment.events();
        OptionEvaluation options = assessment.optionEvaluation().orElse(null);
        return new AssessmentResponse(
                assessment.bookingId(),
                assessment.assessedAt().toString(),
                assessment.decision().name(),
                assessment.reviewReasons().stream()
                        .map(r -> new ReviewReasonView(r.code().name(), r.detail()))
                        .toList(),
                DisruptionView.from(assessment.disruption()),
                assessment.policyEvaluation().map(PolicyView::from).orElse(null),
                options == null ? null : options.bestCandidate().map(OptionView::from).orElse(null),
                options == null ? List.of() : options.rejected().stream()
                        .map(r -> new RejectedOptionView(r.optionId(), r.reasons().stream().map(Enum::name).toList()))
                        .toList(),
                events.isReliable(),
                events.statusBySegment().values().stream().map(SegmentView::from).toList(),
                events.discardedEvents().stream()
                        .map(d -> new DiscardedEventView(d.eventId(), d.source(), d.reason().name()))
                        .toList());
    }

    public record ReviewReasonView(String code, String detail) {
    }

    public record DisruptionView(String type, boolean relevant, String segmentId, int delayMinutes,
                                 ConnectionView connection) {

        static DisruptionView from(Disruption disruption) {
            return new DisruptionView(
                    disruption.type().name(),
                    disruption.isRelevant(),
                    disruption.segmentId(),
                    disruption.delayMinutes(),
                    disruption.affectedConnection().map(ConnectionView::from).orElse(null));
        }
    }

    public record ConnectionView(String arrivingSegmentId, String departingSegmentId, String connectionAirport,
                                 String projectedArrival, String connectionDeparture, long availableMinutes,
                                 boolean missed) {

        static ConnectionView from(ConnectionImpact impact) {
            return new ConnectionView(
                    impact.arrivingSegmentId(),
                    impact.departingSegmentId(),
                    impact.connectionAirport(),
                    impact.projectedArrival().toString(),
                    impact.connectionDeparture().toString(),
                    impact.availableTime().toMinutes(),
                    impact.missed());
        }
    }

    public record PolicyView(String version, List<AppliedRuleView> appliedRules, List<String> undeterminedRules) {

        static PolicyView from(PolicyEvaluation policy) {
            return new PolicyView(
                    policy.policyVersion(),
                    policy.appliedRules().stream()
                            .map(r -> new AppliedRuleView(r.ruleId(), r.entitlement(), r.requiresHumanApproval()))
                            .toList(),
                    policy.undeterminedRuleIds());
        }
    }

    public record AppliedRuleView(String ruleId, String entitlement, boolean requiresHumanApproval) {
    }

    public record OptionView(String optionId, String type, String flightNumber, String date, String departure,
                             String confidence) {

        static OptionView from(EligibleOption option) {
            return new OptionView(
                    option.optionId(),
                    option.type(),
                    option.flightNumber(),
                    option.date() == null ? null : option.date().toString(),
                    option.departure() == null ? null : option.departure().toString(),
                    option.confidence().name());
        }
    }

    public record RejectedOptionView(String optionId, List<String> reasons) {
    }

    public record SegmentView(String segmentId, int delayMinutes, String estimatedDeparture, String gate,
                              List<String> evidenceEventIds) {

        static SegmentView from(SegmentStatus status) {
            return new SegmentView(
                    status.segmentId(),
                    status.delayMinutes(),
                    status.estimatedDeparture().toString(),
                    status.gate(),
                    status.evidenceEventIds());
        }
    }

    public record DiscardedEventView(String eventId, String source, String reason) {
    }
}
