package com.example.travelad.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AirlineService {

    private static final Map<String, String> AIRLINE_NAMES = new HashMap<>();

    // Pre-loaded fallback map to avoid database/getter compilation errors entirely
    static {
        AIRLINE_NAMES.put("LY", "El Al");
        AIRLINE_NAMES.put("DL", "Delta Air Lines");
        AIRLINE_NAMES.put("UA", "United Airlines");
        AIRLINE_NAMES.put("AA", "American Airlines");
        AIRLINE_NAMES.put("BA", "British Airways");
        AIRLINE_NAMES.put("AF", "Air France");
        AIRLINE_NAMES.put("LH", "Lufthansa");
        AIRLINE_NAMES.put("EK", "Emirates");
        AIRLINE_NAMES.put("QR", "Qatar Airways");
        AIRLINE_NAMES.put("EY", "Etihad Airways");
        AIRLINE_NAMES.put("TK", "Turkish Airlines");
        AIRLINE_NAMES.put("AC", "Air Canada");
        AIRLINE_NAMES.put("FR", "Ryanair");
        AIRLINE_NAMES.put("U2", "easyJet");
        AIRLINE_NAMES.put("W6", "Wizz Air");
        AIRLINE_NAMES.put("IZ", "Arkia");
        AIRLINE_NAMES.put("6H", "Israir");
    }

    public String getAirlineNameByCode(String code) {
        if (code == null || code.isEmpty()) {
            return "Unknown Airline";
        }
        return AIRLINE_NAMES.getOrDefault(code, code);
    }

    // Restored the method exactly as FlightsController/Mock expects
    public String getAirlineLogoUrl(String code) {
        return "https://images.kiwi.com/airlines/64/" + code + ".png";
    }
}