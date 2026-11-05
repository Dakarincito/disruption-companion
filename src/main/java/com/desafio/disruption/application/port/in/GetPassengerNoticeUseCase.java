package com.desafio.disruption.application.port.in;

import com.desafio.disruption.domain.model.PassengerNotice;

public interface GetPassengerNoticeUseCase {

    PassengerNotice passengerNotice(String bookingId);
}
