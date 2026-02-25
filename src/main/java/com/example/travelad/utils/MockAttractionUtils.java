package com.example.travelad.utils;

import java.util.Arrays;
import java.util.List;

public class MockAttractionUtils {

    // Handpicked IDs of generic urban architecture, cityscapes, and monuments.
    // Zero animals, zero random forests. This ensures the UI looks professional.
    private static final List<String> ATTRACTION_PHOTO_IDS = Arrays.asList(
            "1477959858617-67f8516f1b1c", // City street
            "1449844908441-8829872d2607", // Architecture
            "1518398092300-5e6fa6e67ca0", // City buildings
            "1499856871958-5b9627545d1a", // Monument style
            "1514565131-ececb8f926af",    // Historic street
            "1480796927426-f609979314bd", // Modern architecture
            "1497362943212-043e0d8601c4", // Museum style building
            "1552832230-c0197dd311b5",    // Urban square
            "1444723121692-4181f2b60098", // Bridge/City
            "1523633589114-88e221458b27"  // Classic columns
    );

    public static String getImageUrlForIndex(int index) {
        String photoId = ATTRACTION_PHOTO_IDS.get(index % ATTRACTION_PHOTO_IDS.size());
        return "https://images.unsplash.com/photo-" + photoId + "?auto=format&fit=crop&w=800&q=80";
    }
}