package com.project.monitoring.controller;

import com.project.monitoring.dto.ApplicationMetricsDTO;
import com.project.monitoring.dto.HealthStatusDTO;
import com.project.monitoring.dto.PerformanceMetricsDTO;
import com.project.monitoring.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * Get overall health status of all services
     * GET /api/monitoring/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatusDTO> getHealth() {
        log.info("Received request to get health status");
        HealthStatusDTO health = metricsService.getServiceHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get application metrics
     * GET /api/monitoring/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApplicationMetricsDTO> getMetrics() {
        log.info("Received request to get application metrics");
        ApplicationMetricsDTO metrics = metricsService.getApplicationMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get performance metrics (throughput, latency, failure rate)
     * GET /api/monitoring/performance-metrics
     * @param minutes Time window in minutes (optional, default: 5)
     */
    @GetMapping("/performance-metrics")
    public ResponseEntity<PerformanceMetricsDTO> getPerformanceMetrics(
            @RequestParam(required = false, defaultValue = "5") Integer minutes) {
        log.info("Received request to get performance metrics for last {} minutes", minutes);
        PerformanceMetricsDTO performanceMetrics = metricsService.getPerformanceMetrics(minutes);
        return ResponseEntity.ok(performanceMetrics);
    }
}
