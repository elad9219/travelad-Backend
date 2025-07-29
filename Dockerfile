FROM openjdk:11
COPY target/travelad-0.0.1-SNAPSHOT.jar /usr/src/travelad.jar
COPY src/main/resources/application.properties /opt/conf/application.properties
# Add volume support for configuration files
VOLUME /opt/conf
# Optional health check to ensure the application is running
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
CMD ["java", "-jar", "/usr/src/travelad.jar", "--spring.config.location=file:/opt/conf/application.properties"]