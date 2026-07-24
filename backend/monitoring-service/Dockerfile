# Build stage - Optimized with layer caching
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom files first for better layer caching
COPY common-lib/pom.xml common-lib/pom.xml
COPY monitoring-service/pom.xml monitoring-service/pom.xml

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN cd common-lib && mvn dependency:go-offline -B
RUN cd monitoring-service && mvn dependency:go-offline -B

# Copy source code
COPY common-lib/src common-lib/src
COPY monitoring-service/src monitoring-service/src

# Build common-lib and monitoring-service
RUN cd common-lib && mvn clean install -DskipTests -B
RUN cd monitoring-service && mvn clean package -DskipTests -B

# Runtime stage - Optimized with security best practices
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy jar from build stage
COPY --from=build --chown=appuser:appgroup /app/monitoring-service/target/*.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

# Optimized JVM settings for production
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+ParallelRefProcEnabled", \
  "-XX:+UseStringDeduplication", \
  "-XX:+OptimizeStringConcat", \
  "-XX:+UseCompressedOops", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.backgroundpreinitializer.ignore=true", \
  "-jar", "app.jar"]
