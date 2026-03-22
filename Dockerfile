# Step 1: Build the application using Maven
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app
# העתקה של כל הפרויקט פנימה - דוקר כבר ימצא לבד את מה שצריך
COPY . .
# בנייה של ה-JAR תוך דילוג על טסטים (קריטי ב-Render כי אין לו גישה לדאטה-בייס בזמן הבנייה)
RUN mvn clean package -DskipTests

# Step 2: Run the application
FROM openjdk:11-jre-slim
WORKDIR /app
# העתקה של הקובץ שנוצר מהשלב הקודם
COPY --from=build /app/target/*.jar app.jar
# הרצה
ENTRYPOINT ["java", "-jar", "app.jar"]