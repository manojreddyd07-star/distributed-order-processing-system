package com.project.common.util;

/**
 * Utility class for common validation operations
 */
public class ValidationUtil {
    
    private ValidationUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Check if a string is null or empty (after trimming)
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Check if a string is not null and not empty
     */
    public static boolean isNotEmpty(String value) {
        return !isNullOrEmpty(value);
    }
    
    /**
     * Validate that a required field is not null or empty
     * @throws IllegalArgumentException if validation fails
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be empty");
        }
    }
    
    /**
     * Validate that a required object is not null
     * @throws IllegalArgumentException if validation fails
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be null");
        }
    }
    
    /**
     * Validate that a number is positive
     * @throws IllegalArgumentException if validation fails
     */
    public static void requirePositive(Number value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
