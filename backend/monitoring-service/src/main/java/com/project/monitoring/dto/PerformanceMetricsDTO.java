package com.project.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsDTO {
    
    private LocalDateTime timestamp;
    private ThroughputMetrics throughput;
    private LatencyMetrics latency;
    private FailureMetrics failure;
    private String timeWindow; // e.g., "last_5_minutes", "last_hour"
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThroughputMetrics {
        private Long totalEvents;
        private Double eventsPerSecond;
        private Double eventsPerMinute;
        private Long successfulEvents;
        private Long failedEvents;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatencyMetrics {
        private Double averageProcessingTimeMs;
        private Double averageProcessingTimeSeconds;
        private Long minProcessingTimeMs;
        private Long maxProcessingTimeMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureMetrics {
        private Long totalFailures;
        private Double failureRate; // Percentage
        private Double successRate; // Percentage
        private Long consecutiveFailures;
    }
}
