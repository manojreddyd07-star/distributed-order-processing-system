package com.project.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for JSON serialization and deserialization
 */
public class JsonUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // Register JavaTimeModule for LocalDateTime support
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    private JsonUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Convert object to JSON string
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON to object: {}", e.getMessage());
            throw new RuntimeException("Failed to parse JSON to object", e);
        }
    }
    
    /**
     * Convert JSON string to generic Object
     */
    public static Object fromJson(String json) {
        return fromJson(json, Object.class);
    }
    
    /**
     * Get the ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
