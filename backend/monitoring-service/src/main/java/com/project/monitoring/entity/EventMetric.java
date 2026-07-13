package com.project.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_metrics", indexes = {
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_service_name", columnList = "serviceName")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String serviceName;
    
    @Column(nullable = false)
    private String status; // SUCCESS, FAILED
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @Column(length = 2000)
    private String metadata;
}
