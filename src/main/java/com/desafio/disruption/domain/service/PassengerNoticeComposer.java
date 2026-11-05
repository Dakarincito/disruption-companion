package com.desafio.disruption.domain.service;

import com.desafio.disruption.domain.model.ConnectionImpact;
import com.desafio.disruption.domain.model.Disruption;
import com.desafio.disruption.domain.model.DisruptionAssessment;
import com.desafio.disruption.domain.model.DisruptionType;
import com.desafio.disruption.domain.model.EligibleOption;
import com.desafio.disruption.domain.model.OptionEvaluation;
import com.desafio.disruption.domain.model.PassengerNotice;
import com.desafio.disruption.domain.model.PassengerNotice.NextStep;
import com.desafio.disruption.domain.model.PassengerNotice.Status;
import com.desafio.disruption.domain.model.PolicyEvaluation;
import com.desafio.disruption.domain.model.Segment;
import com.desafio.disruption.domain.model.SegmentStatus;
import com.desafio.disruption.domain.model.Trip;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//aca creo los metodos para la composicion del aviso al pasajero osea con info verificable
//y las opciones disponibles asi como eventos
public final class PassengerNoticeComposer {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public PassengerNotice compose(Trip trip, DisruptionAssessment assessment) {
        if (!assessment.events().isReliable()) {
            return new PassengerNotice(Status.UNDER_VERIFICATION,
                    "Estamos verificando el estado de tu vuelo",
                    "Recibimos información que estamos confirmando. Te avisaremos apenas tengamos datos verificados.",
                    List.of(), NextStep.AWAIT_AGENT, null);
        }
        List<String> details = gateDetails(trip, assessment);

        return switch (assessment.decision()) {
            case NO_ACTION -> new PassengerNotice(Status.NO_CHANGES,
                    "Sin cambios en tu itinerario",
                    "No registramos cambios relevantes en tu viaje.",
                    details, NextStep.NONE, null);

            case INFORM_PASSENGER -> assessment.disruption().type() == DisruptionType.GATE_CHANGE
                    ? new PassengerNotice(Status.INFORMATION, "Cambio de puerta de embarque",
                    "Hay un cambio en la puerta de embarque de tu vuelo.",
                    details, NextStep.CHECK_FOR_UPDATES, null)
                    : new PassengerNotice(Status.DELAYED, "Tu vuelo tiene un nuevo horario",
                    situation(trip, assessment), details, NextStep.CHECK_FOR_UPDATES, null);

            case RECOMMEND_TO_PASSENGER -> new PassengerNotice(statusOf(assessment.disruption()),
                    titleOf(trip, assessment.disruption()),
                    join(situation(trip, assessment), entitlement(assessment), offer(assessment)),
                    details, NextStep.CONFIRM_OPTION, policyRef(assessment));

            case REQUIRES_HUMAN -> {
                String entitlement = entitlement(assessment);
                yield new PassengerNotice(statusOf(assessment.disruption()),
                        titleOf(trip, assessment.disruption()),
                        join(situation(trip, assessment), entitlement, agentReview(assessment)),
                        details, NextStep.AWAIT_AGENT, entitlement.isEmpty() ? null : policyRef(assessment));
            }
        };
    }

    private static String situation(Trip trip, DisruptionAssessment assessment) {
        Disruption disruption = assessment.disruption();
        if (!disruption.isRelevant()) {
            return "";
        }
        Segment segment = trip.segment(disruption.segmentId()).orElseThrow();
        SegmentStatus status = assessment.events().status(segment.segmentId());
        String delay = "Tu vuelo %s (%s-%s) tiene una nueva hora estimada de salida a las %s (programada a las %s, hora local)"
                .formatted(segment.flightNumber(), segment.origin(), segment.destination(),
                        status.estimatedDeparture().format(TIME), segment.scheduledDeparture().format(TIME));

        if (disruption.type() == DisruptionType.MISSED_CONNECTION) {
            ConnectionImpact connection = disruption.connection();
            Segment next = trip.segment(connection.departingSegmentId()).orElseThrow();
            return delay + ", por lo que no alcanzarías tu conexión %s (%s-%s)."
                    .formatted(next.flightNumber(), next.origin(), next.destination());
        }
        return delay + ".";
    }

    private static String entitlement(DisruptionAssessment assessment) {
        return grants(assessment, PolicyEvaluation.REBOOK_NO_FEE) ? "Según la política vigente, puedes cambiar tu vuelo sin costo." : "";
    }

    private static String offer(DisruptionAssessment assessment) {
        EligibleOption option = assessment.optionEvaluation()
                .flatMap(OptionEvaluation::bestCandidate)
                .orElseThrow(() -> new IllegalStateException("A recommendation requires a candidate option"));
        String price = option.hasFareDifference()
                ? "con una diferencia de tarifa de %s %s".formatted(option.fareDifference().toPlainString(), option.currency())
                : "sin costo adicional";
        return "Te ofrecemos el vuelo %s (%s-%s) del %s a las %s, %s. Confírmalo para gestionarlo."
                .formatted(option.flightNumber(), option.origin(), option.destination(),
                        option.date().format(DATE), option.departure().format(TIME), price);
    }

    private static String agentReview(DisruptionAssessment assessment) {
        return grants(assessment, PolicyEvaluation.PRIORITY_HANDLING)
                ? "Un agente revisará tu caso con prioridad antes de confirmar cualquier cambio."
                : "Un agente revisará tu caso antes de confirmar cualquier cambio.";
    }

    private static List<String> gateDetails(Trip trip, DisruptionAssessment assessment) {
        List<String> details = new ArrayList<>();
        for (Segment segment : trip.segments()) {
            String gate = assessment.events().status(segment.segmentId()).gate();
            if (gate != null) {
                details.add("Puerta de embarque del vuelo %s: %s".formatted(segment.flightNumber(), gate));
            }
        }
        return details;
    }

    private static Status statusOf(Disruption disruption) {
        return disruption.type() == DisruptionType.MISSED_CONNECTION ? Status.CONNECTION_AFFECTED : Status.DELAYED;
    }

    private static String titleOf(Trip trip, Disruption disruption) {
        return disruption.type() == DisruptionType.MISSED_CONNECTION
                ? "Tu conexión a %s está afectada".formatted(trip.finalDestination())
                : "Tu vuelo tiene un nuevo horario";
    }

    private static boolean grants(DisruptionAssessment assessment, String entitlement) {
        return assessment.policyEvaluation().map(p -> p.grants(entitlement)).orElse(false);
    }

    private static String policyRef(DisruptionAssessment assessment) {
        return assessment.policyEvaluation().map(PolicyEvaluation::policyVersion).orElse(null);
    }

    private static String join(String... parts) {
        return String.join(" ", parts).replaceAll("\\s+", " ").trim();
    }
}
