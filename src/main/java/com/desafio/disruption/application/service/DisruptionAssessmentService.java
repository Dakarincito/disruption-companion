package com.desafio.disruption.application.service;

import com.desafio.disruption.application.exception.BookingNotFoundException;
import com.desafio.disruption.application.port.in.AssessDisruptionUseCase;
import com.desafio.disruption.application.port.in.GetPassengerNoticeUseCase;
import com.desafio.disruption.application.port.out.EligibleOptionsPort;
import com.desafio.disruption.application.port.out.OperationalEventsPort;
import com.desafio.disruption.application.port.out.PolicyRulesPort;
import com.desafio.disruption.application.port.out.TripContextPort;
import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.DisruptionAssessment;
import com.desafio.disruption.domain.model.EventResolution;
import com.desafio.disruption.domain.model.OperationalSnapshot;
import com.desafio.disruption.domain.model.OptionEvaluation;
import com.desafio.disruption.domain.model.PassengerNotice;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.PolicySet;
import com.desafio.disruption.domain.model.Trip;
import com.desafio.disruption.domain.service.ConnectionAnalyzer;
import com.desafio.disruption.domain.service.DecisionMaker;
import com.desafio.disruption.domain.service.DisruptionClassifier;
import com.desafio.disruption.domain.service.EventResolver;
import com.desafio.disruption.domain.service.OptionValidator;
import com.desafio.disruption.domain.service.PassengerNoticeComposer;
import com.desafio.disruption.domain.service.PolicyEvaluator;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class DisruptionAssessmentService implements AssessDisruptionUseCase, GetPassengerNoticeUseCase {

    private static final String DELAY_SCENARIO = "DELAY";

    private final TripContextPort tripContextPort;
    private final OperationalEventsPort operationalEventsPort;
    private final PolicyRulesPort policyRulesPort;
    private final EligibleOptionsPort eligibleOptionsPort;
    private final EventResolver eventResolver;
    private final ConnectionAnalyzer connectionAnalyzer;
    private final DisruptionClassifier disruptionClassifier;
    private final PolicyEvaluator policyEvaluator;
    private final OptionValidator optionValidator;
    private final DecisionMaker decisionMaker;
    private final PassengerNoticeComposer noticeComposer;
    private final Clock clock;

    public DisruptionAssessmentService(TripContextPort tripContextPort,
                                       OperationalEventsPort operationalEventsPort,
                                       PolicyRulesPort policyRulesPort,
                                       EligibleOptionsPort eligibleOptionsPort,
                                       EventResolver eventResolver,
                                       ConnectionAnalyzer connectionAnalyzer,
                                       DisruptionClassifier disruptionClassifier,
                                       PolicyEvaluator policyEvaluator,
                                       OptionValidator optionValidator,
                                       DecisionMaker decisionMaker,
                                       PassengerNoticeComposer noticeComposer,
                                       Clock clock) {
        this.tripContextPort = Objects.requireNonNull(tripContextPort);
        this.operationalEventsPort = Objects.requireNonNull(operationalEventsPort);
        this.policyRulesPort = Objects.requireNonNull(policyRulesPort);
        this.eligibleOptionsPort = Objects.requireNonNull(eligibleOptionsPort);
        this.eventResolver = Objects.requireNonNull(eventResolver);
        this.connectionAnalyzer = Objects.requireNonNull(connectionAnalyzer);
        this.disruptionClassifier = Objects.requireNonNull(disruptionClassifier);
        this.policyEvaluator = Objects.requireNonNull(policyEvaluator);
        this.optionValidator = Objects.requireNonNull(optionValidator);
        this.decisionMaker = Objects.requireNonNull(decisionMaker);
        this.noticeComposer = Objects.requireNonNull(noticeComposer);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public DisruptionAssessment assess(String bookingId) {
        return assess(loadTrip(bookingId));
    }

    @Override
    public PassengerNotice passengerNotice(String bookingId) {
        Trip trip = loadTrip(bookingId);
        return noticeComposer.compose(trip, assess(trip));
    }

    private Trip loadTrip(String bookingId) {
        return tripContextPort.findTrip(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    private DisruptionAssessment assess(Trip trip) {
        OperationalSnapshot snapshot = operationalEventsPort.findEvents(trip.segmentIds());
        Instant now = clock.instant();

        EventResolution events = eventResolver.resolve(trip, snapshot, now);
        List<ConnectionImpact> connections = connectionAnalyzer.analyze(trip, events);
        Disruption disruption = disruptionClassifier.classify(events, connections);

        PolicyEvaluation policy = null;
        OptionEvaluation options = null;
        if (disruption.isRelevant()) {
            PolicySet policySet = policyRulesPort.findRules(DELAY_SCENARIO);
            policy = policyEvaluator.evaluate(policySet, trip, events, connections);
            options = optionValidator.validate(eligibleOptionsPort.findOptions(trip), trip, disruption,
                    policySet, policy, now);
        }

        DecisionMaker.Outcome outcome = decisionMaker.decide(trip, disruption, events, policy, options);

        return new DisruptionAssessment(trip.bookingId(), now, disruption, events, connections,
                policy, options, outcome.decision(), outcome.reasons());
    }
}
