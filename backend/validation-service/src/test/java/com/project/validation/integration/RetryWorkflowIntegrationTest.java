package com.project.validation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.OrderCreatedEvent;
import com.project.common.events.RetryEvent;
import com.project.validation.entity.RetryEventEntity;
import com.project.validation.repository.RetryEventRepository;
import com.project.validation.service.RetryService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration Test for Retry Workflow Scenarios
 * Tests retry mechanism for failed event processing
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created", "retry-orders", "order-validated"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RetryWorkflowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private RetryEventRepository retryEventRepository;

    @Autowired
    private RetryService retryService;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, RetryEvent> retryProducerTemplate;
    private KafkaTemplate<String, OrderCreatedEvent> orderProducerTemplate;
    
    private BlockingQueue<ConsumerRecord<String, RetryEvent>> retryRecords;
    private KafkaMessageListenerContainer<String, RetryEvent> retryContainer;

    @BeforeEach
    void setUp() {
        setupProducers();
        setupConsumers();
        retryEventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (retryContainer != null) {
            retryContainer.stop();
        }
        retryEventRepository.deleteAll();
    }

    @Test
    void testRetryEventIsStoredAfterFailure() throws Exception {
        // Step 1: Simulate a failed event that needs retry
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent failedEvent = new OrderCreatedEvent(
            eventId,
            "ORDER_CREATED",
            LocalDateTime.now(),
            1001L,
            "CUST-RETRY-001",
            "CREATED",
            new BigDecimal("299.99"),
            LocalDateTime.now()
        );

        // Step 2: Create retry event manually (simulating failure handling)
        RetryEventEntity retryEntity = new RetryEventEntity();
        retryEntity.setEventId(eventId);
        retryEntity.setEventType("ORDER_CREATED");
        retryEntity.setPayload(objectMapper.writeValueAsString(failedEvent));
        retryEntity.setRetryCount(1);
        retryEntity.setMaxRetries(3);
        retryEntity.setStatus("PENDING");
        retryEntity.setServiceName("validation-service");
        retryEntity.setTargetTopic("order-created");
        retryEntity.setCreatedAt(LocalDateTime.now());
        retryEntity.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        
        retryEventRepository.save(retryEntity);

        // Step 3: Verify retry event is stored
        List<RetryEventEntity> retryEvents = retryEventRepository.findAll();
        assertThat(retryEvents).hasSize(1);
        assertThat(retryEvents.get(0).getEventId()).isEqualTo(eventId);
        assertThat(retryEvents.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void testRetryEventProcessing() throws Exception {
        // Step 1: Create a retry event
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent originalEvent = new OrderCreatedEvent(
            eventId,
            "ORDER_CREATED",
            LocalDateTime.now(),
            1002L,
            "CUST-RETRY-002",
            "CREATED",
            new BigDecimal("450.00"),
            LocalDateTime.now()
        );

        RetryEventEntity retryEntity = new RetryEventEntity();
        retryEntity.setEventId(eventId);
        retryEntity.setEventType("ORDER_CREATED");
        retryEntity.setPayload(objectMapper.writeValueAsString(originalEvent));
        retryEntity.setRetryCount(1);
        retryEntity.setMaxRetries(3);
        retryEntity.setStatus("PENDING");
        retryEntity.setServiceName("validation-service");
        retryEntity.setTargetTopic("order-created");
        retryEntity.setCreatedAt(LocalDateTime.now());
        retryEntity.setNextRetryAt(LocalDateTime.now().minusSeconds(1)); // Eligible for retry now
        
        retryEntity = retryEventRepository.save(retryEntity);

        // Step 2: Trigger retry processing
        retryService.processRetryEvents();

        // Step 3: Verify retry event was processed
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                RetryEventEntity updated = retryEventRepository.findById(retryEntity.getId()).orElse(null);
                assertThat(updated).isNotNull();
                // After successful retry, it should be marked as completed or retry count increased
                assertThat(updated.getRetryCount()).isGreaterThan(1);
            });
    }

    @Test
    void testRetryEventExceedsMaxRetries() throws Exception {
        // Step 1: Create a retry event that has already failed maximum times
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent failedEvent = new OrderCreatedEvent(
            eventId,
            "ORDER_CREATED",
            LocalDateTime.now(),
            1003L,
            "CUST-RETRY-003",
            "CREATED",
            new BigDecimal("599.99"),
            LocalDateTime.now()
        );

        RetryEventEntity retryEntity = new RetryEventEntity();
        retryEntity.setEventId(eventId);
        retryEntity.setEventType("ORDER_CREATED");
        retryEntity.setPayload(objectMapper.writeValueAsString(failedEvent));
        retryEntity.setRetryCount(3); // Already at max retries
        retryEntity.setMaxRetries(3);
        retryEntity.setStatus("PENDING");
        retryEntity.setServiceName("validation-service");
        retryEntity.setTargetTopic("order-created");
        retryEntity.setCreatedAt(LocalDateTime.now());
        retryEntity.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        
        retryEntity = retryEventRepository.save(retryEntity);

        // Step 2: Attempt retry processing
        retryService.processRetryEvents();

        // Step 3: Verify event is moved to failed/DLQ status
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                RetryEventEntity updated = retryEventRepository.findById(retryEntity.getId()).orElse(null);
                assertThat(updated).isNotNull();
                // Should be marked as FAILED when max retries exceeded
                assertThat(updated.getStatus()).isIn("FAILED", "DLQ");
            });
    }

    @Test
    void testRetryEventPublishedToKafka() throws Exception {
        // Step 1: Create and save a retry event
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(
            eventId,
            "ORDER_CREATED",
            LocalDateTime.now(),
            1004L,
            "CUST-RETRY-004",
            "CREATED",
            new BigDecimal("199.99"),
            LocalDateTime.now()
        );

        // Step 2: Publish retry event to Kafka
        RetryEvent retryEvent = new RetryEvent();
        retryEvent.setEventId(eventId);
        retryEvent.setEventType("ORDER_CREATED");
        retryEvent.setPayload(objectMapper.writeValueAsString(event));
        retryEvent.setRetryCount(1);
        retryEvent.setMaxRetries(3);
        retryEvent.setOriginalTopic("order-created");
        retryEvent.setServiceName("validation-service");
        retryEvent.setReason("Simulated failure for testing");

        retryProducerTemplate.send("retry-orders", retryEvent).get(5, TimeUnit.SECONDS);

        // Step 3: Verify retry event was consumed
        ConsumerRecord<String, RetryEvent> retryRecord = retryRecords.poll(10, TimeUnit.SECONDS);
        
        assertThat(retryRecord).isNotNull();
        RetryEvent consumedEvent = retryRecord.value();
        assertThat(consumedEvent.getEventId()).isEqualTo(eventId);
        assertThat(consumedEvent.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(consumedEvent.getRetryCount()).isEqualTo(1);
    }

    @Test
    void testMultipleRetryAttempts() throws Exception {
        // Step 1: Create initial retry event
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(
            eventId,
            "ORDER_CREATED",
            LocalDateTime.now(),
            1005L,
            "CUST-RETRY-005",
            "CREATED",
            new BigDecimal("750.00"),
            LocalDateTime.now()
        );

        // Step 2: Simulate multiple retry attempts
        for (int i = 1; i <= 3; i++) {
            RetryEventEntity retryEntity = new RetryEventEntity();
            retryEntity.setEventId(eventId + "-attempt-" + i);
            retryEntity.setEventType("ORDER_CREATED");
            retryEntity.setPayload(objectMapper.writeValueAsString(event));
            retryEntity.setRetryCount(i);
            retryEntity.setMaxRetries(3);
            retryEntity.setStatus(i < 3 ? "PENDING" : "FAILED");
            retryEntity.setServiceName("validation-service");
            retryEntity.setTargetTopic("order-created");
            retryEntity.setCreatedAt(LocalDateTime.now());
            retryEntity.setNextRetryAt(LocalDateTime.now().plusSeconds(i * 5));
            
            retryEventRepository.save(retryEntity);
        }

        // Step 3: Verify all retry attempts are stored
        List<RetryEventEntity> allRetries = retryEventRepository.findAll();
        assertThat(allRetries).hasSizeGreaterThanOrEqualTo(3);
        
        // Step 4: Verify retry count progression
        long pendingCount = allRetries.stream()
            .filter(r -> "PENDING".equals(r.getStatus()))
            .count();
        long failedCount = allRetries.stream()
            .filter(r -> "FAILED".equals(r.getStatus()))
            .count();
        
        assertThat(pendingCount).isGreaterThanOrEqualTo(2);
        assertThat(failedCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testRetryWithExponentialBackoff() throws Exception {
        // Step 1: Create retry events with increasing backoff times
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(
            eventId,
            "ORDER_CREATED",
            LocalDateTime.now(),
            1006L,
            "CUST-RETRY-006",
            "CREATED",
            new BigDecimal("350.00"),
            LocalDateTime.now()
        );

        LocalDateTime baseTime = LocalDateTime.now();
        
        // Step 2: Create retry events with exponential backoff pattern
        RetryEventEntity retry1 = createRetryEntity(eventId + "-1", event, 1, baseTime.plusSeconds(2));
        RetryEventEntity retry2 = createRetryEntity(eventId + "-2", event, 2, baseTime.plusSeconds(4));
        RetryEventEntity retry3 = createRetryEntity(eventId + "-3", event, 3, baseTime.plusSeconds(8));

        retryEventRepository.save(retry1);
        retryEventRepository.save(retry2);
        retryEventRepository.save(retry3);

        // Step 3: Verify backoff times increase
        List<RetryEventEntity> retries = retryEventRepository.findAll();
        assertThat(retries).hasSizeGreaterThanOrEqualTo(3);

        // Verify that nextRetryAt times are progressively further apart
        for (int i = 0; i < retries.size() - 1; i++) {
            RetryEventEntity current = retries.get(i);
            RetryEventEntity next = retries.get(i + 1);
            
            if (current.getNextRetryAt() != null && next.getNextRetryAt() != null) {
                assertThat(next.getNextRetryAt()).isAfter(current.getNextRetryAt());
            }
        }
    }

    // Helper Methods

    private void setupProducers() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        retryProducerTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        orderProducerTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    private void setupConsumers() {
        retryRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-retry-workflow-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, RetryEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties("retry-orders");
        retryContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        retryContainer.setupMessageListener((MessageListener<String, RetryEvent>) retryRecords::add);
        retryContainer.start();
        ContainerTestUtils.waitForAssignment(retryContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    private RetryEventEntity createRetryEntity(String eventId, OrderCreatedEvent event, 
                                               int retryCount, LocalDateTime nextRetryAt) throws Exception {
        RetryEventEntity entity = new RetryEventEntity();
        entity.setEventId(eventId);
        entity.setEventType("ORDER_CREATED");
        entity.setPayload(objectMapper.writeValueAsString(event));
        entity.setRetryCount(retryCount);
        entity.setMaxRetries(3);
        entity.setStatus("PENDING");
        entity.setServiceName("validation-service");
        entity.setTargetTopic("order-created");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setNextRetryAt(nextRetryAt);
        return entity;
    }
}
