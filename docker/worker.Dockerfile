FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from the worker-service module target directory
COPY ../worker-service/target/*.jar app.jar

# Worker doesn't expose ports, but good practice to define entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]