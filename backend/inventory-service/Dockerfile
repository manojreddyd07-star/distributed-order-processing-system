# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy and build common-lib first
COPY common-lib/pom.xml common-lib/pom.xml
COPY common-lib/src common-lib/src
RUN cd common-lib && mvn clean install -DskipTests

# Copy and build inventory-service
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY inventory-service/src inventory-service/src
RUN cd inventory-service && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/inventory-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
