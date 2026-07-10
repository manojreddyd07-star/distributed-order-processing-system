package com.project.monitoring.service;

import com.project.monitoring.dto.ApplicationMetricsDTO;
import com.project.monitoring.dto.HealthStatusDTO;
import com.project.monitoring.dto.ServiceHealthDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;
    private final RestTemplate restTemplate;

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
}
