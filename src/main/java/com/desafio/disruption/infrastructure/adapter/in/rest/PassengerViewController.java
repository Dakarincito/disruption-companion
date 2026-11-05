package com.desafio.disruption.infrastructure.adapter.in.rest;

import com.desafio.disruption.application.port.in.GetPassengerNoticeUseCase;
import com.desafio.disruption.infrastructure.adapter.in.rest.dto.PassengerViewResponse;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class PassengerViewController {

    private final GetPassengerNoticeUseCase getPassengerNotice;

    public PassengerViewController(GetPassengerNoticeUseCase getPassengerNotice) {
        this.getPassengerNotice = getPassengerNotice;
    }

    @GetMapping("/{bookingId}/passenger-view")
    public PassengerViewResponse passengerView(@PathVariable @Pattern(regexp = BookingIdFormat.PATTERN) String bookingId) {
        return PassengerViewResponse.from(bookingId, getPassengerNotice.passengerNotice(bookingId));
    }
}
