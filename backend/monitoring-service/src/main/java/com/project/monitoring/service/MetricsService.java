package com.project.monitoring.service;

import com.project.monitoring.dto.ApplicationMetricsDTO;
import com.project.monitoring.dto.HealthStatusDTO;
import com.project.monitoring.dto.PerformanceMetricsDTO;
import com.project.monitoring.dto.ServiceHealthDTO;
import com.project.monitoring.entity.EventMetric;
import com.project.monitoring.repository.EventMetricRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;
    private final RestTemplate restTemplate;
    private final EventMetricRepository eventMetricRepository;
    private final MeterRegistry meterRegistry;

    @Value("${monitoring.services.order-service.url:http://localhost:8081}")
    private String orderServiceUrl;

    @Value("${monitoring.services.validation-service.url:http://localhost:8082}")
    private String validationServiceUrl;

    @Value("${monitoring.services.payment-service.url:http://localhost:8083}")
    private String paymentServiceUrl;

    @Value("${monitoring.services.inventory-service.url:http://localhost:8084}")
    private String inventoryServiceUrl;

    @Value("${monitoring.services.fulfillment-service.url:http://localhost:8085}")
    private String fulfillmentServiceUrl;

    /**
     * Get overall service health status
     */
    public HealthStatusDTO getServiceHealth() {
        log.info("Collecting service health status");

        var health = healthEndpoint.health();
        
        return HealthStatusDTO.builder()
                .overallStatus(health.getStatus().getCode())
                .timestamp(LocalDateTime.now())
                .databaseHealth(collectDatabaseHealth())
                .kafkaHealth(collectKafkaHealth())
                .servicesHealth(collectServicesHealth())
                .build();
    }

    /**
     * Get application metrics
     */
    public ApplicationMetricsDTO getApplicationMetrics() {
        log.info("Collecting application metrics");

        return ApplicationMetricsDTO.builder()
                .timestamp(LocalDateTime.now())
                .jvmMetrics(collectJvmMetrics())
                .systemMetrics(collectSystemMetrics())
                .httpMetrics(collectHttpMetrics())
                .customMetrics(new HashMap<>())
                .build();
    }

    /**
     * Collect database health status
     */
    private ServiceHealthDTO collectDatabaseHealth() {
        try {
            var health = healthEndpoint.health();
            var dbHealth = health.getComponents().get("db");
            
            if (dbHealth != null) {
                Map<String, Object> details = new HashMap<>();
                if (dbHealth.getDetails() != null) {
                    details.putAll(dbHealth.getDetails());
                }
                
                return ServiceHealthDTO.builder()
                        .serviceName("PostgreSQL Database")
                        .status(dbHealth.getStatus().getCode())
                        .description("Database connection status")
                        .timestamp(LocalDateTime.now())
                        .details(details)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error collecting database health", e);
        }

        return ServiceHealthDTO.builder()
                .serviceName("PostgreSQL Database")
                .status("UNKNOWN")
                .description("Unable to retrieve database health")
                .timestamp(LocalDateTime.now())
                .details(Map.of("error", "Health check failed"))
                .build();
    }

    /**
     * Collect Kafka health status
     */
    private ServiceHealthDTO collectKafkaHealth() {
        try {
            var health = healthEndpoint.health();
            var kafkaHealth = health.getComponents().get("kafka");
            
            if (kafkaHealth != null) {
                Map<String, Object> details = new HashMap<>();
                if (kafkaHealth.getDetails() != null) {
                    details.putAll(kafkaHealth.getDetails());
                }
                
                return ServiceHealthDTO.builder()
                        .serviceName("Apache Kafka")
                        .status(kafkaHealth.getStatus().getCode())
                        .description("Kafka connection status")
                        .timestamp(LocalDateTime.now())
                        .details(details)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error collecting Kafka health", e);
        }

        return ServiceHealthDTO.builder()
                .serviceName("Apache Kafka")
                .status("UNKNOWN")
                .description("Unable to retrieve Kafka health")
                .timestamp(LocalDateTime.now())
                .details(Map.of("error", "Health check failed"))
                .build();
    }

    /**
     * Collect health status of all microservices
     */
    private List<ServiceHealthDTO> collectServicesHealth() {
        List<ServiceHealthDTO> servicesHealth = new ArrayList<>();

        servicesHealth.add(checkServiceHealth("Order Service", orderServiceUrl));
        servicesHealth.add(checkServiceHealth("Validation Service", validationServiceUrl));
        servicesHealth.add(checkServiceHealth("Payment Service", paymentServiceUrl));
        servicesHealth.add(checkServiceHealth("Inventory Service", inventoryServiceUrl));
        servicesHealth.add(checkServiceHealth("Fulfillment Service", fulfillmentServiceUrl));

        return servicesHealth;
    }

    /**
     * Check health of a specific service
     */
    private ServiceHealthDTO checkServiceHealth(String serviceName, String serviceUrl) {
        try {
            String healthUrl = serviceUrl + "/actuator/health";
            Map<String, Object> response = restTemplate.getForObject(healthUrl, Map.class);
            
            if (response != null) {
                String status = (String) response.get("status");
                
                return ServiceHealthDTO.builder()
                        .serviceName(serviceName)
                        .status(status != null ? status : "UNKNOWN")
                        .description("Service is responding")
                        .timestamp(LocalDateTime.now())
                        .details(response)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error checking health for service: {}", serviceName, e);
        }

        return ServiceHealthDTO.builder()
                .serviceName(serviceName)
                .status("DOWN")
                .description("Service is not reachable")
                .timestamp(LocalDateTime.now())
                .details(Map.of("error", "Unable to connect to service"))
                .build();
    }

    /**
     * Collect JVM metrics
     */
    private ApplicationMetricsDTO.JvmMetrics collectJvmMetrics() {
        try {
            Long memoryUsed = getMetricValue("jvm.memory.used");
            Long memoryMax = getMetricValue("jvm.memory.max");
            Long memoryCommitted = getMetricValue("jvm.memory.committed");
            
            Double memoryUsagePercentage = 0.0;
            if (memoryMax != null && memoryMax > 0 && memoryUsed != null) {
                memoryUsagePercentage = (memoryUsed.doubleValue() / memoryMax.doubleValue()) * 100;
            }

            return ApplicationMetricsDTO.JvmMetrics.builder()
                    .memoryUsed(memoryUsed)
                    .memoryMax(memoryMax)
                    .memoryCommitted(memoryCommitted)
                    .memoryUsagePercentage(memoryUsagePercentage)
                    .threadsLive(getMetricValue("jvm.threads.live").intValue())
                    .threadsPeak(getMetricValue("jvm.threads.peak").intValue())
                    .threadsDaemon(getMetricValue("jvm.threads.daemon").intValue())
                    .build();
        } catch (Exception e) {
            log.error("Error collecting JVM metrics", e);
            return ApplicationMetricsDTO.JvmMetrics.builder().build();
        }
    }

    /**
     * Collect system metrics
     */
    private ApplicationMetricsDTO.SystemMetrics collectSystemMetrics() {
        try {
            Double cpuUsage = getMetricDoubleValue("system.cpu.usage");
            Integer cpuCount = getMetricValue("system.cpu.count").intValue();
            Double systemLoadAverage = getMetricDoubleValue("system.load.average.1m");

            return ApplicationMetricsDTO.SystemMetrics.builder()
                    .cpuUsage(cpuUsage != null ? cpuUsage * 100 : 0.0)
                    .cpuCount(cpuCount)
                    .systemLoadAverage(systemLoadAverage)
                    .build();
        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
            return ApplicationMetricsDTO.SystemMetrics.builder().build();
        }
    }

    /**
     * Collect HTTP metrics
     */
    private ApplicationMetricsDTO.HttpMetrics collectHttpMetrics() {
        try {
            Long totalRequests = getMetricValue("http.server.requests");
            
            return ApplicationMetricsDTO.HttpMetrics.builder()
                    .totalRequests(totalRequests)
                    .successfulRequests(0L)
                    .failedRequests(0L)
                    .averageResponseTime(0.0)
                    .build();
        } catch (Exception e) {
            log.error("Error collecting HTTP metrics", e);
            return ApplicationMetricsDTO.HttpMetrics.builder().build();
        }
    }

    /**
     * Get metric value as Long
     */
    private Long getMetricValue(String metricName) {
        try {
            var metric = metricsEndpoint.metric(metricName, null);
            if (metric != null && metric.getMeasurements() != null && !metric.getMeasurements().isEmpty()) {
                return metric.getMeasurements().get(0).getValue().longValue();
            }
        } catch (Exception e) {
            log.debug("Metric {} not available", metricName);
        }
        return 0L;
    }

    /**
     * Get metric value as Double
     */
    private Double getMetricDoubleValue(String metricName) {
        try {
            var metric = metricsEndpoint.metric(metricName, null);
            if (metric != null && metric.getMeasurements() != null && !metric.getMeasurements().isEmpty()) {
                return metric.getMeasurements().get(0).getValue();
            }
        } catch (Exception e) {
            log.debug("Metric {} not available", metricName);
        }
        return 0.0;
    }

    /**
     * Get performance metrics for a specified time window
     * @param minutes Time window in minutes (default: 5)
     */
    public PerformanceMetricsDTO getPerformanceMetrics(Integer minutes) {
        log.info("Collecting performance metrics for last {} minutes", minutes);
        
        int timeWindow = minutes != null ? minutes : 5;
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(timeWindow);
        
        return PerformanceMetricsDTO.builder()
                .timestamp(LocalDateTime.now())
                .timeWindow("last_" + timeWindow + "_minutes")
                .throughput(calculateThroughput(startTime, endTime, timeWindow))
                .latency(calculateProcessingLatency(startTime, endTime))
                .failure(calculateFailureRate(startTime, endTime))
                .build();
    }

    /**
     * Calculate throughput metrics
     */
    public PerformanceMetricsDTO.ThroughputMetrics calculateThroughput(
            LocalDateTime startTime, LocalDateTime endTime, int timeWindowMinutes) {
        try {
            long totalEvents = eventMetricRepository.countByTimestampBetween(startTime, endTime);
            long successfulEvents = eventMetricRepository.countByStatusAndTimestampBetween("SUCCESS", startTime, endTime);
            long failedEvents = eventMetricRepository.countByStatusAndTimestampBetween("FAILED", startTime, endTime);
            
            // Calculate events per second and per minute
            long durationSeconds = ChronoUnit.SECONDS.between(startTime, endTime);
            double eventsPerSecond = durationSeconds > 0 ? (double) totalEvents / durationSeconds : 0.0;
            double eventsPerMinute = timeWindowMinutes > 0 ? (double) totalEvents / timeWindowMinutes : 0.0;
            
            // Update Prometheus metrics
            Counter.builder("monitoring.throughput.total")
                    .description("Total number of events processed")
                    .register(meterRegistry)
                    .increment(totalEvents);
            
            Counter.builder("monitoring.throughput.success")
                    .description("Total successful events")
                    .register(meterRegistry)
                    .increment(successfulEvents);
            
            Counter.builder("monitoring.throughput.failed")
                    .description("Total failed events")
                    .register(meterRegistry)
                    .increment(failedEvents);
            
            log.info("Throughput calculated - Total: {}, Success: {}, Failed: {}, Events/sec: {}", 
                    totalEvents, successfulEvents, failedEvents, eventsPerSecond);
            
            return PerformanceMetricsDTO.ThroughputMetrics.builder()
                    .totalEvents(totalEvents)
                    .eventsPerSecond(Math.round(eventsPerSecond * 100.0) / 100.0)
                    .eventsPerMinute(Math.round(eventsPerMinute * 100.0) / 100.0)
                    .successfulEvents(successfulEvents)
                    .failedEvents(failedEvents)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating throughput metrics", e);
            return PerformanceMetricsDTO.ThroughputMetrics.builder()
                    .totalEvents(0L)
                    .eventsPerSecond(0.0)
                    .eventsPerMinute(0.0)
                    .successfulEvents(0L)
                    .failedEvents(0L)
                    .build();
        }
    }

    /**
     * Calculate processing latency metrics
     */
    public PerformanceMetricsDTO.LatencyMetrics calculateProcessingLatency(
            LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<EventMetric> events = eventMetricRepository.findByTimestampBetween(startTime, endTime);
            
            if (events.isEmpty()) {
                log.info("No events found for latency calculation");
                return PerformanceMetricsDTO.LatencyMetrics.builder()
                        .averageProcessingTimeMs(0.0)
                        .averageProcessingTimeSeconds(0.0)
                        .minProcessingTimeMs(0L)
                        .maxProcessingTimeMs(0L)
                        .build();
            }
            
            // Calculate average, min, max processing times
            Double avgProcessingTime = eventMetricRepository.getAverageProcessingTime(startTime, endTime);
            
            Long minProcessingTime = events.stream()
                    .map(EventMetric::getProcessingTimeMs)
                    .filter(Objects::nonNull)
                    .min(Long::compareTo)
                    .orElse(0L);
            
            Long maxProcessingTime = events.stream()
                    .map(EventMetric::getProcessingTimeMs)
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L);
            
            double avgMs = avgProcessingTime != null ? avgProcessingTime : 0.0;
            double avgSeconds = avgMs / 1000.0;
            
            // Update Prometheus metrics
            Timer.builder("monitoring.processing.latency")
                    .description("Event processing latency")
                    .register(meterRegistry)
                    .record(java.time.Duration.ofMillis(avgMs.longValue()));
            
            log.info("Latency calculated - Avg: {} ms, Min: {} ms, Max: {} ms", avgMs, minProcessingTime, maxProcessingTime);
            
            return PerformanceMetricsDTO.LatencyMetrics.builder()
                    .averageProcessingTimeMs(Math.round(avgMs * 100.0) / 100.0)
                    .averageProcessingTimeSeconds(Math.round(avgSeconds * 100.0) / 100.0)
                    .minProcessingTimeMs(minProcessingTime)
                    .maxProcessingTimeMs(maxProcessingTime)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating latency metrics", e);
            return PerformanceMetricsDTO.LatencyMetrics.builder()
                    .averageProcessingTimeMs(0.0)
                    .averageProcessingTimeSeconds(0.0)
                    .minProcessingTimeMs(0L)
                    .maxProcessingTimeMs(0L)
                    .build();
        }
    }

    /**
     * Calculate failure rate metrics
     */
    public PerformanceMetricsDTO.FailureMetrics calculateFailureRate(
            LocalDateTime startTime, LocalDateTime endTime) {
        try {
            long totalEvents = eventMetricRepository.countByTimestampBetween(startTime, endTime);
            long totalFailures = eventMetricRepository.countByStatusAndTimestampBetween("FAILED", startTime, endTime);
            long totalSuccess = eventMetricRepository.countByStatusAndTimestampBetween("SUCCESS", startTime, endTime);
            
            // Calculate failure and success rates
            double failureRate = totalEvents > 0 ? ((double) totalFailures / totalEvents) * 100 : 0.0;
            double successRate = totalEvents > 0 ? ((double) totalSuccess / totalEvents) * 100 : 0.0;
            
            // Calculate consecutive failures
            List<EventMetric> recentEvents = eventMetricRepository.findByTimestampBetween(
                    endTime.minusMinutes(10), endTime);
            recentEvents.sort(Comparator.comparing(EventMetric::getTimestamp).reversed());
            
            long consecutiveFailures = 0;
            for (EventMetric event : recentEvents) {
                if ("FAILED".equals(event.getStatus())) {
                    consecutiveFailures++;
                } else {
                    break;
                }
            }
            
            // Update Prometheus metrics
            meterRegistry.gauge("monitoring.failure.rate", failureRate);
            meterRegistry.gauge("monitoring.success.rate", successRate);
            
            log.info("Failure rate calculated - Failures: {}, Total: {}, Failure Rate: {}%, Consecutive: {}", 
                    totalFailures, totalEvents, failureRate, consecutiveFailures);
            
            return PerformanceMetricsDTO.FailureMetrics.builder()
                    .totalFailures(totalFailures)
                    .failureRate(Math.round(failureRate * 100.0) / 100.0)
                    .successRate(Math.round(successRate * 100.0) / 100.0)
                    .consecutiveFailures(consecutiveFailures)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating failure rate metrics", e);
            return PerformanceMetricsDTO.FailureMetrics.builder()
                    .totalFailures(0L)
                    .failureRate(0.0)
                    .successRate(0.0)
                    .consecutiveFailures(0L)
                    .build();
        }
    }

    /**
     * Record an event metric (to be called when events are processed)
     */
    public void recordEventMetric(String eventType, String serviceName, String status, 
                                  Long processingTimeMs, String orderId, String errorMessage) {
        try {
            EventMetric eventMetric = EventMetric.builder()
                    .eventType(eventType)
                    .serviceName(serviceName)
                    .status(status)
                    .timestamp(LocalDateTime.now())
                    .processingTimeMs(processingTimeMs)
                    .orderId(orderId)
                    .errorMessage(errorMessage)
                    .build();
            
            eventMetricRepository.save(eventMetric);
            log.debug("Event metric recorded: {} - {} - {}", eventType, serviceName, status);
        } catch (Exception e) {
            log.error("Error recording event metric", e);
        }
    }
}
