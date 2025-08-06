# Step 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl product-service -am

# Step 2: Run the application
FROM eclipse-temurin:21-jdk
WORKDIR /app
# COPY --from=build /app/target/product-service-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/product-service/target/product-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
