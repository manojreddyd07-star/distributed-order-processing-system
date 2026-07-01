# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy and build common-lib first
COPY common-lib/pom.xml common-lib/pom.xml
COPY common-lib/src common-lib/src
RUN cd common-lib && mvn clean install -DskipTests

# Copy and build order-service
COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src
RUN cd order-service && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
