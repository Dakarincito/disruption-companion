package com.desafio.disruption.application.exception;

public class BookingNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookingNotFoundException(String bookingId) {
        super("Booking " + bookingId + " not found");
    }
}
