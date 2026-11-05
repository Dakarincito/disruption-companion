package com.desafio.disruption.application.port.in;

import com.desafio.disruption.domain.model.DisruptionAssessment;

public interface AssessDisruptionUseCase {

    DisruptionAssessment assess(String bookingId);
}
