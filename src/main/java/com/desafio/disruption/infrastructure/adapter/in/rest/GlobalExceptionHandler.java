package com.desafio.disruption.infrastructure.adapter.in.rest;

import com.desafio.disruption.application.exception.BookingNotFoundException;
import com.desafio.disruption.application.port.out.ExternalToolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BookingNotFoundException.class)
    public ProblemDetail handleBookingNotFound(BookingNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Booking not found");
        return problem;
    }

    @ExceptionHandler(ExternalToolException.class)
    public ProblemDetail handleExternalTool(ExternalToolException e) {
        log.error("Tool {} failed: {}", e.tool(), e.getMessage(), e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "A data source returned invalid data or is unavailable. The assessment was stopped.");
        problem.setTitle("Upstream data error");
        return problem;
    }
}
