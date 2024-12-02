package com.example.travelad.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class InputValidator {

    public static void validateDate(String date, String fieldName) {
        if (date != null && !date.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate.parse(date, formatter);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(fieldName + " has an invalid date format. Expected format: yyyy-MM-dd");
            }
        }
    }

    public static void validateHotelIds(String hotelIds) {
        if (hotelIds == null || hotelIds.trim().isEmpty()) {
            throw new IllegalArgumentException("Hotel IDs cannot be null or empty.");
        }
        if (!hotelIds.matches("^([A-Za-z0-9]+,)*[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("Hotel IDs must be comma-separated alphanumeric values.");
        }
    }
}
