package com.project.validation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.FailedEvent;
import com.project.common.events.OrderCreatedEvent;
import com.project.validation.entity.FailedEventEntity;
import com.project.validation.repository.FailedEventRepository;
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
 * Integration test for Kafka DLQ (Dead Letter Queue) Scenarios
 * Tests DLQ mechanism for permanently failed events
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created", "dlq-failed-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaDLQScenarioIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private FailedEventRepository failedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, FailedEvent> dlqProducerTemplate;
    private BlockingQueue<ConsumerRecord<String, FailedEvent>> dlqRecords;
    private KafkaMessageListenerContainer<String, FailedEvent> dlqContainer;

    @BeforeEach
    void setUp() {
        // Setup producer to send FailedEvent to DLQ
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, FailedEvent> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        dlqProducerTemplate = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify FailedEvent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlq-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, FailedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("dlq-failed-events");
        dlqContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        dlqRecords = new LinkedBlockingQueue<>();
        dlqContainer.setupMessageListener((MessageListener<String, FailedEvent>) dlqRecords::add);
        dlqContainer.start();

        ContainerTestUtils.waitForAssignment(dlqContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (dlqContainer != null) {
            dlqContainer.stop();
        }
        if (failedEventRepository != null) {
            failedEventRepository.deleteAll();
        }
    }

    @Test
    void testDLQScenario_FailedEventShouldBePublishedToDLQ() throws Exception {
        // Arrange - Create a failed event that exhausted retries
        OrderCreatedEvent failedOrder = createOrderCreatedEvent(1L, null, new BigDecimal("99.99"));
        String eventPayload = objectMapper.writeValueAsString(failedOrder);
        
        FailedEvent failedEvent = createFailedEvent(
            failedOrder.getEventId(),
            "ORDER_CREATED",
            "validation-service",
            "Customer ID is required - Max retries exhausted",
            eventPayload
        );

        // Act
        dlqProducerTemplate.send("dlq-failed-events", "1", failedEvent);

        // Assert - Verify failed event is published to DLQ
        ConsumerRecord<String, FailedEvent> record = dlqRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("dlq-failed-events");
        
        FailedEvent receivedEvent = record.value();
        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.getEventId()).isEqualTo(failedOrder.getEventId());
        assertThat(receivedEvent.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(receivedEvent.getServiceName()).isEqualTo("validation-service");
        assertThat(receivedEvent.getErrorMessage()).contains("Max retries exhausted");
    }

    @Test
    void testDLQScenario_FailedEventShouldBeStoredInDatabase() throws Exception {
        // Arrange
        OrderCreatedEvent failedOrder = createOrderCreatedEvent(2L, null, new BigDecimal("50.00"));
        String eventPayload = objectMapper.writeValueAsString(failedOrder);
        
        FailedEvent failedEvent = createFailedEvent(
            failedOrder.getEventId(),
            "ORDER_CREATED",
            "validation-service",
            "Validation failed after 3 retries",
            eventPayload
        );

        // Act
        dlqProducerTemplate.send("dlq-failed-events", "2", failedEvent);

        // Assert - Wait for failed event to be stored in database
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<FailedEventEntity> failedEvents = failedEventRepository.findByServiceName("validation-service");
                assertThat(failedEvents).isNotEmpty();
                
                FailedEventEntity storedEvent = failedEvents.stream()
                    .filter(e -> e.getEventId().equals(failedOrder.getEventId()))
                    .findFirst()
                    .orElse(null);
                    
                assertThat(storedEvent).isNotNull();
                assertThat(storedEvent.getEventType()).isEqualTo("ORDER_CREATED");
                assertThat(storedEvent.getErrorMessage()).contains("Validation failed");
            });
    }

    @Test
    void testDLQScenario_VerifyFailedEventMetadata() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent failedOrder = createOrderCreatedEvent(3L, null, new BigDecimal("75.00"));
        String eventPayload = objectMapper.writeValueAsString(failedOrder);
        
        FailedEvent failedEvent = new FailedEvent();
        failedEvent.setEventId(eventId);
        failedEvent.setEventType("ORDER_CREATED");
        failedEvent.setServiceName("validation-service");
        failedEvent.setErrorMessage("Invalid order data - Customer ID missing");
        failedEvent.setPayload(eventPayload);
        failedEvent.setFailedAt(LocalDateTime.now());

        // Act
        dlqProducerTemplate.send("dlq-failed-events", "3", failedEvent);

        // Assert - Verify all metadata is preserved
        ConsumerRecord<String, FailedEvent> record = dlqRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        FailedEvent receivedEvent = record.value();
        assertThat(receivedEvent.getEventId()).isEqualTo(eventId);
        assertThat(receivedEvent.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(receivedEvent.getServiceName()).isEqualTo("validation-service");
        assertThat(receivedEvent.getErrorMessage()).contains("Customer ID missing");
        assertThat(receivedEvent.getPayload()).isNotNull();
        assertThat(receivedEvent.getFailedAt()).isNotNull();
    }

    @Test
    void testDLQScenario_MultipleFailedEvents_ShouldStoreAll() throws Exception {
        // Arrange - Create multiple failed events
        OrderCreatedEvent failed1 = createOrderCreatedEvent(4L, null, new BigDecimal("100.00"));
        OrderCreatedEvent failed2 = createOrderCreatedEvent(5L, null, new BigDecimal("150.00"));
        OrderCreatedEvent failed3 = createOrderCreatedEvent(6L, null, new BigDecimal("200.00"));
        
        String payload1 = objectMapper.writeValueAsString(failed1);
        String payload2 = objectMapper.writeValueAsString(failed2);
        String payload3 = objectMapper.writeValueAsString(failed3);
        
        FailedEvent event1 = createFailedEvent(failed1.getEventId(), "ORDER_CREATED", "validation-service", "Failed 1", payload1);
        FailedEvent event2 = createFailedEvent(failed2.getEventId(), "ORDER_CREATED", "validation-service", "Failed 2", payload2);
        FailedEvent event3 = createFailedEvent(failed3.getEventId(), "ORDER_CREATED", "validation-service", "Failed 3", payload3);

        // Act
        dlqProducerTemplate.send("dlq-failed-events", "4", event1);
        dlqProducerTemplate.send("dlq-failed-events", "5", event2);
        dlqProducerTemplate.send("dlq-failed-events", "6", event3);

        // Assert - Verify all failed events are stored
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<FailedEventEntity> failedEvents = failedEventRepository.findByServiceName("validation-service");
                assertThat(failedEvents.size()).isGreaterThanOrEqualTo(3);
            });

        ConsumerRecord<String, FailedEvent> record1 = dlqRecords.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, FailedEvent> record2 = dlqRecords.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, FailedEvent> record3 = dlqRecords.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
        assertThat(record3).isNotNull();
    }

    @Test
    void testDLQScenario_FailedEventPayloadRetrieval() throws Exception {
        // Arrange
        OrderCreatedEvent failedOrder = createOrderCreatedEvent(7L, null, new BigDecimal("250.00"));
        String eventPayload = objectMapper.writeValueAsString(failedOrder);
        
        FailedEvent failedEvent = createFailedEvent(
            failedOrder.getEventId(),
            "ORDER_CREATED",
            "validation-service",
            "Processing error",
            eventPayload
        );

        // Act
        dlqProducerTemplate.send("dlq-failed-events", "7", failedEvent);

        // Assert - Verify payload can be retrieved and deserialized
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<FailedEventEntity> failedEvents = failedEventRepository.findByEventId(failedOrder.getEventId());
                assertThat(failedEvents).isNotEmpty();
                
                FailedEventEntity storedEvent = failedEvents.get(0);
                assertThat(storedEvent.getPayload()).isNotNull();
                
                // Verify payload can be deserialized back to OrderCreatedEvent
                OrderCreatedEvent deserializedOrder = objectMapper.readValue(
                    storedEvent.getPayload(), 
                    OrderCreatedEvent.class
                );
                assertThat(deserializedOrder.getOrderId()).isEqualTo(7L);
                assertThat(deserializedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
            });
    }

    @Test
    void testDLQScenario_VerifyTimestamp() throws Exception {
        // Arrange
        LocalDateTime beforePublish = LocalDateTime.now();
        OrderCreatedEvent failedOrder = createOrderCreatedEvent(8L, null, new BigDecimal("300.00"));
        String eventPayload = objectMapper.writeValueAsString(failedOrder);
        
        FailedEvent failedEvent = createFailedEvent(
            failedOrder.getEventId(),
            "ORDER_CREATED",
            "validation-service",
            "Timestamp test",
            eventPayload
        );

        // Act
        dlqProducerTemplate.send("dlq-failed-events", "8", failedEvent);

        // Assert - Verify timestamp is set correctly
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<FailedEventEntity> failedEvents = failedEventRepository.findByEventId(failedOrder.getEventId());
                assertThat(failedEvents).isNotEmpty();
                
                FailedEventEntity storedEvent = failedEvents.get(0);
                assertThat(storedEvent.getFailedAt()).isAfter(beforePublish.minusSeconds(1));
                assertThat(storedEvent.getFailedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
            });
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

    private FailedEvent createFailedEvent(String eventId, String eventType, String serviceName,
                                         String errorMessage, String payload) {
        FailedEvent failedEvent = new FailedEvent();
        failedEvent.setEventId(eventId);
        failedEvent.setEventType(eventType);
        failedEvent.setServiceName(serviceName);
        failedEvent.setErrorMessage(errorMessage);
        failedEvent.setPayload(payload);
        failedEvent.setFailedAt(LocalDateTime.now());
        return failedEvent;
    }
}
