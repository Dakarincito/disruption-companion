package com.desafio.disruption.infrastructure.adapter.in.rest.dto;

import com.desafio.disruption.domain.model.PassengerNotice;

import java.util.List;

public record PassengerViewResponse(
        String bookingId,
        String status,
        String title,
        String message,
        List<String> details,
        String nextStep,
        String policyRef) {

    public static PassengerViewResponse from(String bookingId, PassengerNotice notice) {
        return new PassengerViewResponse(
                bookingId,
                notice.status().name(),
                notice.title(),
                notice.message(),
                notice.details(),
                notice.nextStep().name(),
                notice.policyRef());
    }
}
