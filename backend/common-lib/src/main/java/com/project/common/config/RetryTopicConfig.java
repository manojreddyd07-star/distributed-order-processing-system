package com.project.common.config;

/**
 * Configuration constants for retry topic
 */
public final class RetryTopicConfig {
    
    // Topic name
    public static final String RETRY_TOPIC = "retry-orders";
    
    // Retry configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long BASE_RETRY_INTERVAL_MS = 5000; // 5 seconds
    public static final long RETRY_INTERVAL_MULTIPLIER = 2; // Exponential backoff
    
    // Retry intervals for each attempt (exponential backoff)
    public static final long RETRY_INTERVAL_1 = BASE_RETRY_INTERVAL_MS; // 5 seconds
    public static final long RETRY_INTERVAL_2 = BASE_RETRY_INTERVAL_MS * RETRY_INTERVAL_MULTIPLIER; // 10 seconds
    public static final long RETRY_INTERVAL_3 = BASE_RETRY_INTERVAL_MS * RETRY_INTERVAL_MULTIPLIER * RETRY_INTERVAL_MULTIPLIER; // 20 seconds
    
    // Private constructor to prevent instantiation
    private RetryTopicConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Calculate the retry interval based on retry count
     * @param retryCount Current retry attempt number
     * @return Retry interval in milliseconds
     */
    public static long getRetryInterval(int retryCount) {
        if (retryCount <= 0) {
            return RETRY_INTERVAL_1;
        } else if (retryCount == 1) {
            return RETRY_INTERVAL_2;
        } else {
            return RETRY_INTERVAL_3;
        }
    }
    
    /**
     * Check if retry attempts have been exhausted
     * @param retryCount Current retry count
     * @return true if max retries reached, false otherwise
     */
    public static boolean isMaxRetriesReached(int retryCount) {
        return retryCount >= MAX_RETRY_ATTEMPTS;
    }
}
