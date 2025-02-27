package com.example.travelad.utils;

import com.example.travelad.service.AirlineService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AirlineServiceStatic implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static String getAirlineLogoUrl(String iataCode) {
        AirlineService airlineService = context.getBean(AirlineService.class);
        return airlineService.getAirlineLogoUrl(iataCode);
    }
}