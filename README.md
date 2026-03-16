# **Travelad \- Comprehensive Travel Planning Platform**

Travelad is a robust, full-stack travel planning application that provides users with an all-in-one interactive dashboard. Users can search for any city worldwide and instantly receive a consolidated view of maps, weather, flights, hotels, and local attractions.

Built with an emphasis on resilient architecture, the platform features a multi-layered caching system, dynamic fallback mechanisms for missing data, and a custom data aggregation engine.

## **Quick Links**

* **Live Demo**: [https://traveladd.runmydocker-app.com/](https://traveladd.runmydocker-app.com/)  
* **API Documentation (Swagger)**: [https://traveladd.runmydocker-app.com/swagger-ui.html](https://traveladd.runmydocker-app.com/swagger-ui.html)  
* **Backend Repository**: [https://github.com/elad9219/travelad-backend](https://github.com/elad9219/travelad-backend)  
* **Frontend Repository**: [https://github.com/elad9219/travelad-frontend](https://github.com/elad9219/travelad-frontend)

## **Table of Contents**

* [Features](https://www.google.com/search?q=%23features)  
* [Architecture & Technical Highlights](https://www.google.com/search?q=%23architecture--technical-highlights)  
* [Technologies](https://www.google.com/search?q=%23technologies)  
* [Project Structure](https://www.google.com/search?q=%23project-structure)  
* [Screenshots](https://www.google.com/search?q=%23screenshots)  
* [Installation & Local Setup](https://www.google.com/search?q=%23installation--local-setup)  
* [Contact](https://www.google.com/search?q=%23contact)

## **Features**

* **Interactive 6-Tile Dashboard**: A seamless Single Page Application (SPA) displaying synchronized data for Flights, Hotels, Attractions, Weather, Maps, and Place Details.  
* **Smart City Search**: Global city search with intelligent autocomplete based on extensive IATA/City code mappings.  
* **Personalized Search History**: Automatically tracks and displays recently searched destinations.  
* **Resilient UI/UX**: Graceful degradation and custom placeholder components (Fallbacks) ensure a clean UI even when external APIs return partial or missing media.

## **Architecture & Technical Highlights**

* **Custom Aggregation Engine (Attractions)**: Combines coordinate-based boundary searches via Geoapify API with media enrichment from the Wikipedia API. Includes a custom Regex filter to ensure only relevant Latin-character articles are queried, preventing data pollution.  
* **Mock Data Engine**: A highly structured local simulation engine for Flights and Hotels, designed to exactly mirror real-world GDS (Global Distribution System) responses. Built as a scalable foundation for future integration with live Affiliate APIs (e.g., Viator, GetYourGuide).  
* **Self-Healing Multi-Layer Cache**: Utilizes Redis for high-speed volatile caching and PostgreSQL for persistent data storage. The backend autonomously re-fetches data from external APIs if database records are found incomplete or missing.  
* **Error Handling**: Comprehensive global exception handling (@ControllerAdvice) preventing terminal flooding during external API timeouts (e.g., 504 Gateway errors).

## **Technologies**

### **Backend**

* **Java 11** & **Spring Boot**  
* **PostgreSQL** (Relational Database)  
* **Redis** (In-Memory Data Store / Cache)  
* **Spring Data JPA** & **Hibernate**  
* **Swagger / SpringFox** (API Documentation)

### **Frontend**

* **React.js** with **TypeScript**  
* **Axios** (HTTP Client)  
* **CSS3** (Responsive Grid/Flexbox Layouts)

### **DevOps & External Services**

* **Docker** (Containerization & Deployment)  
* **Google Places API** & **Google Maps**  
* **Geoapify API** (Geocoding & Places)  
* **WeatherAPI**  
* **Wikipedia API** (Media Enrichment)

## Project Structure

### Backend (`elad9219/travelad-backend`)

```
travelad-backend/
├── pom.xml
├── Dockerfile
└── src/
    └── main/
        ├── java/com/example/travelad/
        │   ├── TraveladApplication.java
        │   ├── advice/
        │   │   ├── ApiAdvice.java
        │   │   └── ErrorDetail.java
        │   ├── beans/
        │   │   ├── Airline.java
        │   │   ├── AirlineCacheStatus.java
        │   │   ├── Attraction.java
        │   │   ├── AttractionCacheStatus.java
        │   │   ├── GooglePlaces.java
        │   │   ├── Hotel.java
        │   │   ├── HotelCacheStatus.java
        │   │   └── IataCodeEntry.java
        │   ├── config/
        │   │   ├── AppConfig.java
        │   │   ├── AsyncConfig.java
        │   │   ├── CORS.java
        │   │   ├── RedisConfig.java
        │   │   └── SwaggerConfig.java
        │   ├── controller/
        │   │   ├── AttractionsController.java
        │   │   ├── CityCacheController.java
        │   │   ├── FlightsController.java
        │   │   ├── GooglePlacesController.java
        │   │   ├── HotelsController.java
        │   │   ├── IataCodesController.java
        │   │   └── WeatherController.java
        │   ├── dto/
        │   │   ├── AttractionDto.java
        │   │   ├── FlightOfferDto.java
        │   │   ├── FlightSegmentDto.java
        │   │   ├── HotelDto.java
        │   │   ├── HotelOffersDto.java
        │   │   ├── LocationDto.java
        │   │   ├── RoomDto.java
        │   │   └── WeatherDto.java
        │   ├── exceptions/
        │   │   ├── ExternalApiException.java
        │   │   ├── GlobalExceptionHandler.java
        │   │   └── InvalidInputException.java
        │   ├── repositories/
        │   │   ├── AirlineCacheStatusRepository.java
        │   │   ├── AirlineRepository.java
        │   │   ├── AttractionCacheStatusRepository.java
        │   │   ├── AttractionRepository.java
        │   │   ├── GooglePlacesRepository.java
        │   │   ├── HotelCacheStatusRepository.java
        │   │   └── HotelRepository.java
        │   ├── service/
        │   │   ├── AircraftMapping.java
        │   │   ├── AirlineService.java
        │   │   ├── AsyncAirlineCacheService.java
        │   │   ├── AsyncHotelCacheService.java
        │   │   ├── AttractionsService.java
        │   │   ├── CityCacheService.java
        │   │   ├── FlightsService.java
        │   │   ├── GooglePlacesService.java
        │   │   ├── HotelsService.java
        │   │   └── WeatherService.java
        │   └── utils/
        │       ├── AirlineServiceStatic.java
        │       ├── IataCodeUtils.java
        │       ├── InputValidator.java
        │       ├── MockFlightUtils.java
        │       └── MockHotelUtils.java
        └── resources/
            └── application.properties.example
```



### Frontend (`elad9219/travelad-frontend`)

```
travelad-frontend/
├── package.json
├── tsconfig.json
├── clean-cities.ts
├── public/
│   ├── index.html
│   └── manifest.json
└── src/
    ├── App.tsx
    ├── App.css
    ├── index.tsx
    ├── index.css
    ├── Components/
    │   ├── Components/
    │   │   ├── AttractionComponent/
    │   │   │   ├── AttractionComponent.tsx
    │   │   │   └── AttractionComponent.css
    │   │   ├── FlightComponent/
    │   │   │   ├── FlightComponent.tsx
    │   │   │   └── FlightComponent.css
    │   │   ├── HotelComponent/
    │   │   │   ├── HotelComponent.tsx
    │   │   │   └── HotelComponent.css
    │   │   ├── MapComponent/
    │   │   │   ├── MapComponent.tsx
    │   │   │   └── MapComponent.css
    │   │   ├── SearchBar/
    │   │   │   ├── SearchBar.tsx
    │   │   │   └── SearchBar.css
    │   │   └── WeatherComponent/
    │   │       ├── WeatherComponent.tsx
    │   │       └── WeatherComponent.css
    │   └── PlaceDetails/
    │       └── PlaceDetails/
    │           ├── PlaceDetails.tsx
    │           └── PlaceDetails.css
    ├── modal/
    │   ├── Attraction.ts
    │   ├── City.ts
    │   ├── Flight.ts
    │   ├── Hotel.ts
    │   ├── LocationDto.ts
    │   └── Weather.ts
    └── utils/
        └── globals.ts
```



## **Screenshots**

### **Main Dashboard & Search**

*(Add your screenshot here)*

### **Attractions (Aggregation Engine & Fallbacks)**

*(Add your screenshot here)*

### **Flights & Hotels Simulation**

*(Add your screenshot here)*

### **Interactive Map & Weather**

*(Add your screenshot here)*

## **Installation & Local Setup**

### **Prerequisites**

* Java 11  
* Node.js (v14+)  
* Docker (optional, for Redis/PostgreSQL local containers)  
* Maven

### **Running the Backend**

1. Clone the repository:  
   git clone \[https://github.com/elad9219/travelad-backend.git\](https://github.com/elad9219/travelad-backend.git)

2. Navigate to the project directory and configure your application.properties with your local database credentials and API keys.  
3. Build and run:  
   mvn clean install  
   mvn spring-boot:run

### **Running the Frontend**

1. Clone the repository:  
   git clone \[https://github.com/elad9219/travelad-frontend.git\](https://github.com/elad9219/travelad-frontend.git)

2. Navigate to the directory and install dependencies:  
   npm install

3. Start the development server:  
   npm start

## **Contact**

**Elad Tennenboim**

* GitHub: [elad9219](https://www.google.com/search?q=https://github.com/elad9219)  
* LinkedIn: [Elad Tennenboim](https://www.linkedin.com/in/elad-tennenboim/)  
* Email: elad9219@gmail.com
