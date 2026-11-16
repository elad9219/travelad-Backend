package com.example.travelad.service;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.utils.MockFlightUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
public class FlightsService {

    private static final Logger logger =
            LoggerFactory.getLogger(FlightsService.class);

    public List<FlightOfferDto> flights(
            String origin,
            String destination,
            String departDate,
            String adults,
            String returnDate) {
        String from = airportCode(origin);
        String to = airportCode(destination);

        if (from.equals(to)) {
            throw invalid("Origin and destination must differ");
        }

        String passengerCount = adults == null ? "" : adults.strip();
        if (!passengerCount.matches("[1-9]")) {
            throw invalid("Adults must be between 1 and 9");
        }

        LocalDate departure = date(departDate);
        if (departure.isBefore(LocalDate.now())) {
            throw invalid("Departure date must not be in the past");
        }

        String returning = "";
        if (returnDate != null && !returnDate.isBlank()) {
            LocalDate arrival = date(returnDate);

            if (arrival.isBefore(departure)) {
                throw invalid("Return date must not precede departure");
            }

            returning = arrival.toString();
        }

        logger.info(
                "Generating MOCK flights from {} to {} on {}",
                from,
                to,
                departure
        );

        return MockFlightUtils.generateMockFlights(
                from,
                to,
                departure.toString(),
                returning,
                passengerCount
        );
    }

    private String airportCode(String value) {
        String code = value == null
                ? ""
                : value.strip().toUpperCase(Locale.ROOT);

        if (!code.matches("[A-Z]{3}")) {
            throw invalid("Invalid airport code");
        }

        return code;
    }

    private LocalDate date(String value) {
        if (value == null ||
                !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid("Invalid flight date");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Invalid flight date");
        }
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}