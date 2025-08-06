# Step 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl auth-service -am

# Step 2: Run the application
FROM eclipse-temurin:21-jdk
WORKDIR /app
# COPY --from=build /app/target/auth-service-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
