package com.project.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDTO {
    private String overallStatus;
    private LocalDateTime timestamp;
    private ServiceHealthDTO databaseHealth;
    private ServiceHealthDTO kafkaHealth;
    private List<ServiceHealthDTO> servicesHealth;
}
