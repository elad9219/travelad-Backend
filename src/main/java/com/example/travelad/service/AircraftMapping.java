package com.example.travelad.service;

import java.util.HashMap;
import java.util.Map;

public class AircraftMapping {
    private static final Map<String, String> aircraftMap = new HashMap<>();

    static {
        aircraftMap.put("221", "AIRBUS A220-100");
        aircraftMap.put("321", "AIRBUS A321");
        aircraftMap.put("32Q", "AIRBUS A321NEO");
        aircraftMap.put("7M8", "BOEING 737 MAX 8");
        aircraftMap.put("789", "BOEING 787-9");
        aircraftMap.put("73H", "BOEING 737-800 (WINGLETS)");
        aircraftMap.put("295", "EMBRAER 195 E2");
        aircraftMap.put("73J", "BOEING 737-900");
        aircraftMap.put("7M9", "BOEING 737 MAX 9");
        aircraftMap.put("32N", "A320NEO");
        aircraftMap.put("351", "AIRBUS A350-1000");
        aircraftMap.put("330", "AIRBUS INDUSTRIE A330");
        aircraftMap.put("333", "AIRBUS A330-300");
        aircraftMap.put("777", "BOEING 777-200/300");
        aircraftMap.put("359", "AIRBUS A350-900");
        aircraftMap.put("76W", "BOEING 767-300 (WINGLETS)");
        aircraftMap.put("339", "AIRBUS A330-900NEO PASSENGER");
        aircraftMap.put("E75", "EMBRAER 175");
        aircraftMap.put("CR9", "CANADAIR REGIONAL JET 900");
        aircraftMap.put("781", "BOEING 787-10");
        aircraftMap.put("320", "AIRBUS A320");
        aircraftMap.put("343", "AIRBUS A340-300");
        aircraftMap.put("223", "AIRBUS  A220-300");
        aircraftMap.put("763", "BOEING 767-300/300ER");
        aircraftMap.put("346", "AIRBUS A340-600");
        aircraftMap.put("764", "BOEING 767-400");
        aircraftMap.put("75T", "BOEING 757-300 WINGLETS");
        aircraftMap.put("744", "BOEING 747-400");
        aircraftMap.put("788", "BOEING 787-8");
    }

    public static String getAircraftFullName(String code) {
        return aircraftMap.getOrDefault(code, code);
    }
}
