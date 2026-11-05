package com.desafio.disruption.infrastructure.adapter.in.rest;

import com.desafio.disruption.application.port.in.AssessDisruptionUseCase;
import com.desafio.disruption.infrastructure.adapter.in.rest.dto.AssessmentResponse;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class AssessmentController {

    private final AssessDisruptionUseCase assessDisruption;

    public AssessmentController(AssessDisruptionUseCase assessDisruption) {
        this.assessDisruption = assessDisruption;
    }

    @GetMapping("/{bookingId}/assessment")
    public AssessmentResponse assess(@PathVariable @Pattern(regexp = BookingIdFormat.PATTERN) String bookingId) {
        return AssessmentResponse.from(assessDisruption.assess(bookingId));
    }
}
