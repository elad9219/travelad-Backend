FROM openjdk:11
COPY target/travelad-0.0.1-SNAPSHOT.jar /usr/src/travelad.jar
COPY src/main/resources/application.properties /opt/conf/application.properties
CMD ["java", "-jar", "/usr/src/travelad.jar", "--spring.config.location=file:/opt/conf/application.properties"]