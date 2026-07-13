package com.project.monitoring.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.monitoring.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventMetricsConsumer {
    
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;
    
    /**
     * Listen to all order-related events for metrics collection
     */
    @KafkaListener(
        topics = {
            "order-created", 
            "order-validated", 
            "payment-processed",
            "inventory-reserved",
            "order-fulfilled",
            "validation-failed",
            "payment-failed",
            "inventory-failed",
            "fulfillment-failed"
        },
        groupId = "monitoring-metrics-group"
    )
    public void consumeOrderEvents(String message, Acknowledgment acknowledgment) {
        try {
            log.debug("Received event for metrics: {}", message);
            
            JsonNode jsonNode = objectMapper.readTree(message);
            
            String eventType = extractEventType(jsonNode);
            String serviceName = extractServiceName(eventType);
            String status = eventType.contains("failed") ? "FAILED" : "SUCCESS";
            String orderId = extractOrderId(jsonNode);
            Long processingTimeMs = extractProcessingTime(jsonNode);
            String errorMessage = extractErrorMessage(jsonNode);
            
            // Record the event metric
            metricsService.recordEventMetric(
                eventType, 
                serviceName, 
                status, 
                processingTimeMs, 
                orderId, 
                errorMessage
            );
            
            acknowledgment.acknowledge();
            log.debug("Event metric recorded successfully for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error processing event for metrics", e);
            acknowledgment.acknowledge(); // Acknowledge even on error to avoid blocking
        }
    }
    
    /**
     * Extract event type from message
     */
    private String extractEventType(JsonNode jsonNode) {
        if (jsonNode.has("eventType")) {
            return jsonNode.get("eventType").asText();
        }
        if (jsonNode.has("type")) {
            return jsonNode.get("type").asText();
        }
        return "unknown-event";
    }
    
    /**
     * Determine service name from event type
     */
    private String extractServiceName(String eventType) {
        if (eventType.contains("order")) {
            return "order-service";
        } else if (eventType.contains("validation") || eventType.contains("validated")) {
            return "validation-service";
        } else if (eventType.contains("payment")) {
            return "payment-service";
        } else if (eventType.contains("inventory")) {
            return "inventory-service";
        } else if (eventType.contains("fulfillment") || eventType.contains("fulfilled")) {
            return "fulfillment-service";
        }
        return "unknown-service";
    }
    
    /**
     * Extract order ID from message
     */
    private String extractOrderId(JsonNode jsonNode) {
        if (jsonNode.has("orderId")) {
            return jsonNode.get("orderId").asText();
        }
        if (jsonNode.has("id")) {
            return jsonNode.get("id").asText();
        }
        return "unknown";
    }
    
    /**
     * Extract processing time from message
     */
    private Long extractProcessingTime(JsonNode jsonNode) {
        if (jsonNode.has("processingTimeMs")) {
            return jsonNode.get("processingTimeMs").asLong();
        }
        if (jsonNode.has("processingTime")) {
            return jsonNode.get("processingTime").asLong();
        }
        // Calculate random processing time for demo (50-500ms)
        return 50L + (long) (Math.random() * 450);
    }
    
    /**
     * Extract error message from message
     */
    private String extractErrorMessage(JsonNode jsonNode) {
        if (jsonNode.has("errorMessage")) {
            return jsonNode.get("errorMessage").asText();
        }
        if (jsonNode.has("error")) {
            return jsonNode.get("error").asText();
        }
        if (jsonNode.has("message")) {
            return jsonNode.get("message").asText();
        }
        return null;
    }
}
