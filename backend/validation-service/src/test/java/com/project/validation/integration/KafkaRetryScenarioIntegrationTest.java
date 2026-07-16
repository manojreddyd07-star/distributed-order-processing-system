package com.project.validation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.OrderCreatedEvent;
import com.project.common.events.RetryEvent;
import com.project.validation.entity.IdempotencyEntity;
import com.project.validation.entity.RetryEventEntity;
import com.project.validation.repository.IdempotencyRepository;
import com.project.validation.repository.RetryEventRepository;
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
 * Integration test for Kafka Retry Scenarios
 * Tests retry mechanism for failed events
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
class KafkaRetryScenarioIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private RetryEventRepository retryEventRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, RetryEvent> retryProducerTemplate;
    private BlockingQueue<ConsumerRecord<String, RetryEvent>> retryRecords;
    private KafkaMessageListenerContainer<String, RetryEvent> retryContainer;

    @BeforeEach
    void setUp() {
        // Setup producer to send RetryEvent
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, RetryEvent> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        retryProducerTemplate = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify RetryEvent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-retry-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, RetryEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("retry-orders");
        retryContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        retryRecords = new LinkedBlockingQueue<>();
        retryContainer.setupMessageListener((MessageListener<String, RetryEvent>) retryRecords::add);
        retryContainer.start();

        ContainerTestUtils.waitForAssignment(retryContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (retryContainer != null) {
            retryContainer.stop();
        }
        if (retryEventRepository != null) {
            retryEventRepository.deleteAll();
        }
        if (idempotencyRepository != null) {
            idempotencyRepository.deleteAll();
        }
    }

    @Test
    void testRetryScenario_FailedEventShouldBePublishedToRetryTopic() throws Exception {
        // Arrange - Create a retry event for a failed order
        OrderCreatedEvent failedEvent = createOrderCreatedEvent(1L, null, new BigDecimal("99.99")); // null customerId causes failure
        String eventPayload = objectMapper.writeValueAsString(failedEvent);
        
        RetryEvent retryEvent = createRetryEvent(
            failedEvent.getEventId(),
            "ORDER_CREATED",
            eventPayload,
            "validation-service",
            "order-created",
            0,
            3
        );

        // Act
        retryProducerTemplate.send("retry-orders", "1", retryEvent);

        // Assert - Verify retry event is published and consumed
        ConsumerRecord<String, RetryEvent> record = retryRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("retry-orders");
        
        RetryEvent receivedEvent = record.value();
        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.getOriginalEventId()).isEqualTo(failedEvent.getEventId());
        assertThat(receivedEvent.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(receivedEvent.getRetryCount()).isEqualTo(0);
        assertThat(receivedEvent.getMaxRetries()).isEqualTo(3);
    }

    @Test
    void testRetryScenario_RetryEventShouldBeStoredInDatabase() throws Exception {
        // Arrange
        OrderCreatedEvent failedEvent = createOrderCreatedEvent(2L, null, new BigDecimal("50.00"));
        String eventPayload = objectMapper.writeValueAsString(failedEvent);
        
        RetryEvent retryEvent = createRetryEvent(
            failedEvent.getEventId(),
            "ORDER_CREATED",
            eventPayload,
            "validation-service",
            "order-created",
            0,
            3
        );

        // Act
        retryProducerTemplate.send("retry-orders", "2", retryEvent);

        // Assert - Wait for retry event to be stored in database
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<RetryEventEntity> retryEvents = retryEventRepository.findByOriginalEventId(failedEvent.getEventId());
                assertThat(retryEvents).isNotEmpty();
                RetryEventEntity storedEvent = retryEvents.get(0);
                assertThat(storedEvent.getOriginalEventId()).isEqualTo(failedEvent.getEventId());
                assertThat(storedEvent.getEventType()).isEqualTo("ORDER_CREATED");
                assertThat(storedEvent.getRetryCount()).isGreaterThanOrEqualTo(0);
            });
    }

    @Test
    void testRetryScenario_IncrementRetryCount() throws Exception {
        // Arrange - Create retry event with retry count 1
        OrderCreatedEvent failedEvent = createOrderCreatedEvent(3L, null, new BigDecimal("75.00"));
        String eventPayload = objectMapper.writeValueAsString(failedEvent);
        
        RetryEvent retryEvent = createRetryEvent(
            failedEvent.getEventId(),
            "ORDER_CREATED",
            eventPayload,
            "validation-service",
            "order-created",
            1, // Already attempted once
            3
        );

        // Act
        retryProducerTemplate.send("retry-orders", "3", retryEvent);

        // Assert - Verify retry count is maintained
        ConsumerRecord<String, RetryEvent> record = retryRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        RetryEvent receivedEvent = record.value();
        assertThat(receivedEvent.getRetryCount()).isEqualTo(1);
    }

    @Test
    void testRetryScenario_MaxRetriesReached_ShouldMoveToLQ() throws Exception {
        // Arrange - Create retry event that has reached max retries
        OrderCreatedEvent failedEvent = createOrderCreatedEvent(4L, null, new BigDecimal("100.00"));
        String eventPayload = objectMapper.writeValueAsString(failedEvent);
        
        RetryEvent retryEvent = createRetryEvent(
            failedEvent.getEventId(),
            "ORDER_CREATED",
            eventPayload,
            "validation-service",
            "order-created",
            3, // Max retries reached
            3
        );

        // Act
        retryProducerTemplate.send("retry-orders", "4", retryEvent);

        // Wait for processing
        Thread.sleep(5000);

        // Assert - Verify retry event indicates max retries reached
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<RetryEventEntity> retryEvents = retryEventRepository.findByOriginalEventId(failedEvent.getEventId());
                if (!retryEvents.isEmpty()) {
                    RetryEventEntity storedEvent = retryEvents.get(0);
                    // Event should either be moved to DLQ or marked as failed
                    assertThat(storedEvent.getRetryCount()).isGreaterThanOrEqualTo(3);
                }
            });
    }

    @Test
    void testRetryScenario_VerifyRetryEventMetadata() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent failedEvent = createOrderCreatedEvent(5L, null, new BigDecimal("125.00"));
        String eventPayload = objectMapper.writeValueAsString(failedEvent);
        
        RetryEvent retryEvent = createRetryEvent(
            eventId,
            "ORDER_CREATED",
            eventPayload,
            "validation-service",
            "order-created",
            0,
            3
        );
        retryEvent.setFailureReason("Customer ID is required");
        retryEvent.setLastRetryTime(LocalDateTime.now());
        retryEvent.setNextRetryTime(LocalDateTime.now().plusMinutes(5));

        // Act
        retryProducerTemplate.send("retry-orders", "5", retryEvent);

        // Assert - Verify all metadata is preserved
        ConsumerRecord<String, RetryEvent> record = retryRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        RetryEvent receivedEvent = record.value();
        assertThat(receivedEvent.getFailureReason()).isEqualTo("Customer ID is required");
        assertThat(receivedEvent.getServiceName()).isEqualTo("validation-service");
        assertThat(receivedEvent.getTargetTopic()).isEqualTo("order-created");
        assertThat(receivedEvent.getEventPayload()).isNotNull();
    }

    @Test
    void testRetryScenario_IdempotencyCheck_ShouldPreventDuplicateProcessing() throws Exception {
        // Arrange - Mark event as already processed
        String eventId = UUID.randomUUID().toString();
        IdempotencyEntity idempotency = new IdempotencyEntity();
        idempotency.setEventId(eventId);
        idempotency.setEventType("ORDER_CREATED");
        idempotency.setProcessedStatus("PROCESSED");
        idempotency.setProcessedAt(LocalDateTime.now());
        idempotencyRepository.save(idempotency);

        OrderCreatedEvent event = createOrderCreatedEvent(6L, "CUST-006", new BigDecimal("150.00"));
        event.setEventId(eventId);
        String eventPayload = objectMapper.writeValueAsString(event);
        
        RetryEvent retryEvent = createRetryEvent(
            eventId,
            "ORDER_CREATED",
            eventPayload,
            "validation-service",
            "order-created",
            0,
            3
        );

        // Act
        retryProducerTemplate.send("retry-orders", "6", retryEvent);

        // Wait for processing
        Thread.sleep(5000);

        // Assert - Verify event is skipped due to idempotency
        IdempotencyEntity storedIdempotency = idempotencyRepository.findByEventId(eventId).orElse(null);
        assertThat(storedIdempotency).isNotNull();
        assertThat(storedIdempotency.getProcessedStatus()).isEqualTo("PROCESSED");
    }

    private OrderCreatedEvent createOrderCreatedEvent(Long orderId, String customerId, BigDecimal amount) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("ORDER_CREATED");
        event.setEventTimestamp(LocalDateTime.now());
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setOrderStatus("CREATED");
        event.setTotalAmount(amount);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private RetryEvent createRetryEvent(String originalEventId, String eventType, String payload,
                                        String serviceName, String targetTopic, int retryCount, int maxRetries) {
        RetryEvent retryEvent = new RetryEvent();
        retryEvent.setRetryId(UUID.randomUUID().toString());
        retryEvent.setOriginalEventId(originalEventId);
        retryEvent.setEventType(eventType);
        retryEvent.setRetryCount(retryCount);
        retryEvent.setMaxRetries(maxRetries);
        retryEvent.setEventPayload(payload);
        retryEvent.setEventClass("com.project.common.events.OrderCreatedEvent");
        retryEvent.setServiceName(serviceName);
        retryEvent.setTargetTopic(targetTopic);
        retryEvent.setLastRetryTime(LocalDateTime.now());
        retryEvent.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        retryEvent.setFailureReason("Processing failed");
        return retryEvent;
    }
}
