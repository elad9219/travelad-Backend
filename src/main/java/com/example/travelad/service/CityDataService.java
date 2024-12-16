/*

package com.example.travelad.service;

import com.example.travelad.beans.CityData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityDataService {

    @Autowired
    private CityDataRepository cityDataRepository;

    @Autowired
    private CityAttractionRepository cityAttractionRepository;

    @Autowired
    private GooglePlacesService googlePlacesService;

    @Autowired
    private GeoapifyService geoapifyService;

    public CityData getOrFetchCityData(String cityName) {
        CityData cityData = cityDataRepository.findByCityName(cityName);
        if (cityData == null || cityData.getMapUrl() == null || cityData.getImageUrl() == null) {
            // Fetch map and image from GooglePlacesService
            String placeResponse = googlePlacesService.searchPlace(cityName);
            String placeId = extractPlaceId(placeResponse);
            String placeDetails = googlePlacesService.getPlaceDetails(placeId);

            String mapUrl = extractMapUrl(placeDetails);
            String imageUrl = extractImageUrl(placeDetails);

            if (cityData == null) {
                cityData = new CityData();
                cityData.setCityName(cityName);
            }
            cityData.setMapUrl(mapUrl);
            cityData.setImageUrl(imageUrl);
            cityDataRepository.save(cityData);
        }
        return cityData;
    }

    public List<CityAttraction> getOrFetchCityAttractions(String cityName) {
        CityData cityData = cityDataRepository.findByCityName(cityName);
        if (cityData == null) {
            cityData = getOrFetchCityData(cityName);
        }

        List<CityAttraction> attractions = cityAttractionRepository.findByCityId(cityData.getId());
        if (attractions.isEmpty()) {
            List<GeoapifyPlaceDto> geoapifyAttractions = geoapifyService.searchPlacesByCity(cityName);
            attractions = geoapifyAttractions.stream().map(dto -> {
                CityAttraction attraction = new CityAttraction();
                attraction.setCityId(cityData.getId());
                attraction.setAttractionName(dto.getName());
                attraction.setDescription(dto.getDescription());
                attraction.setAddress(dto.getAddress());
                attraction.setPhone(dto.getPhone());
                attraction.setWebsite(dto.getWebsite());
                attraction.setOpeningHours(dto.getOpening_hours());
                return attraction;
            }).toList();
            cityAttractionRepository.saveAll(attractions);
        }
        return attractions;
    }

    private String extractPlaceId(String placeResponse) {
        // Parse the place ID from the Google Places API response
        return ...;
    }

    private String extractMapUrl(String placeDetails) {
        // Parse the map URL from the Google Places API details response
        return ...;
    }

    private String extractImageUrl(String placeDetails) {
        // Parse the image URL from the Google Places API details response
        return ...;
    }
}


 */

