# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy and build common-lib first
COPY common-lib/pom.xml common-lib/pom.xml
COPY common-lib/src common-lib/src
RUN cd common-lib && mvn clean install -DskipTests

# Copy and build fulfillment-service
COPY fulfillment-service/pom.xml fulfillment-service/pom.xml
COPY fulfillment-service/src fulfillment-service/src
RUN cd fulfillment-service && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/fulfillment-service/target/*.jar app.jar
EXPOSE 8084

# Optimized JVM settings for performance
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+ParallelRefProcEnabled", \
  "-XX:+UseStringDeduplication", \
  "-Xms512m", \
  "-Xmx1024m", \
  "-XX:MetaspaceSize=128m", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
  "-jar", "app.jar"]
