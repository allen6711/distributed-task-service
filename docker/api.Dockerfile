FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from the api-service module target directory
COPY ../api-service/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]