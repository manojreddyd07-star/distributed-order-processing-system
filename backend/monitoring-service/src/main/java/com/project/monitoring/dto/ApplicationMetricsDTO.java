package com.project.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMetricsDTO {
    private LocalDateTime timestamp;
    private JvmMetrics jvmMetrics;
    private SystemMetrics systemMetrics;
    private HttpMetrics httpMetrics;
    private Map<String, Object> customMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmMetrics {
        private Long memoryUsed;
        private Long memoryMax;
        private Long memoryCommitted;
        private Double memoryUsagePercentage;
        private Integer threadsLive;
        private Integer threadsPeak;
        private Integer threadsDaemon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMetrics {
        private Double cpuUsage;
        private Integer cpuCount;
        private Double systemLoadAverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpMetrics {
        private Long totalRequests;
        private Long successfulRequests;
        private Long failedRequests;
        private Double averageResponseTime;
    }
}
