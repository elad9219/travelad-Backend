# שלב 1: בניית הפרויקט
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app
COPY . .
# מדלגים על טסטים כי אין דאטה-בייס בזמן הבנייה
RUN mvn clean package -DskipTests

# שלב 2: הרצת האפליקציה
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# הפקודה הזו אומרת ל-Spring Boot: "קח את ההגדרות הרגילות,
# אבל תשתמש גם בקובץ הסודי שהעלינו ל-Render בנתיב /etc/secrets/"
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/etc/secrets/application.properties"]