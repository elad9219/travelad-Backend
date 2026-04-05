# Step 1: Build the application using Maven
FROM maven:3.8.6-eclipse-temurin-11 AS build
WORKDIR /app
COPY . .
# Skip tests during build phase since database is not available yet
RUN mvn clean package -DskipTests

# Step 2: Run the application
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Start the Spring Boot application normally
ENTRYPOINT ["java", "-jar", "app.jar"]