package com.desafio.disruption.infrastructure.config;

import com.desafio.disruption.application.port.out.EligibleOptionsPort;
import com.desafio.disruption.application.port.out.OperationalEventsPort;
import com.desafio.disruption.application.port.out.PolicyRulesPort;
import com.desafio.disruption.application.port.out.TripContextPort;
import com.desafio.disruption.application.service.DisruptionAssessmentService;
import com.desafio.disruption.domain.service.ConnectionAnalyzer;
import com.desafio.disruption.domain.service.DecisionMaker;
import com.desafio.disruption.domain.service.DisruptionClassifier;
import com.desafio.disruption.domain.service.EventResolver;
import com.desafio.disruption.domain.service.OptionValidator;
import com.desafio.disruption.domain.service.PassengerNoticeComposer;
import com.desafio.disruption.domain.service.PolicyEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@Configuration
public class BeanConfiguration {

    @Bean
    public Clock clock(@Value("${app.clock.fixed-instant:}") String fixedInstant) {
        return fixedInstant.isBlank()
                ? Clock.systemUTC()
                : Clock.fixed(Instant.parse(fixedInstant), ZoneOffset.UTC);
    }

    @Bean
    public EventResolver eventResolver(@Value("${app.assessment.max-event-age-minutes}") long maxEventAgeMinutes) {
        return new EventResolver(Duration.ofMinutes(maxEventAgeMinutes));
    }

    @Bean
    public ConnectionAnalyzer connectionAnalyzer(
            @Value("${app.assessment.minimum-connection-minutes}") long minimumConnectionMinutes) {
        return new ConnectionAnalyzer(Duration.ofMinutes(minimumConnectionMinutes));
    }

    @Bean
    public DisruptionClassifier disruptionClassifier(
            @Value("${app.assessment.relevant-delay-minutes}") int relevantDelayMinutes) {
        return new DisruptionClassifier(relevantDelayMinutes);
    }

    @Bean
    public PolicyEvaluator policyEvaluator() {
        return new PolicyEvaluator();
    }

    @Bean
    public OptionValidator optionValidator(
            @Value("${app.assessment.minimum-connection-minutes}") long minimumConnectionMinutes) {
        return new OptionValidator(Duration.ofMinutes(minimumConnectionMinutes));
    }

    @Bean
    public DecisionMaker decisionMaker() {
        return new DecisionMaker();
    }

    @Bean
    public PassengerNoticeComposer passengerNoticeComposer() {
        return new PassengerNoticeComposer();
    }

    @Bean
    public DisruptionAssessmentService disruptionAssessmentService(TripContextPort tripContextPort,
                                                                   OperationalEventsPort operationalEventsPort,
                                                                   PolicyRulesPort policyRulesPort,
                                                                   EligibleOptionsPort eligibleOptionsPort,
                                                                   EventResolver eventResolver,
                                                                   ConnectionAnalyzer connectionAnalyzer,
                                                                   DisruptionClassifier disruptionClassifier,
                                                                   PolicyEvaluator policyEvaluator,
                                                                   OptionValidator optionValidator,
                                                                   DecisionMaker decisionMaker,
                                                                   PassengerNoticeComposer passengerNoticeComposer,
                                                                   Clock clock) {
        return new DisruptionAssessmentService(tripContextPort, operationalEventsPort, policyRulesPort,
                eligibleOptionsPort, eventResolver, connectionAnalyzer, disruptionClassifier,
                policyEvaluator, optionValidator, decisionMaker, passengerNoticeComposer, clock);
    }
}
