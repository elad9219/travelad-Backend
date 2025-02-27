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
    }

    public static String getAircraftFullName(String code) {
        return aircraftMap.getOrDefault(code, code);
    }
}
