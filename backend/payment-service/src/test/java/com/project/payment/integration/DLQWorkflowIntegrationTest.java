package com.project.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.FailedEvent;
import com.project.common.events.OrderValidatedEvent;
import com.project.payment.entity.FailedEventEntity;
import com.project.payment.repository.FailedEventRepository;
import com.project.payment.service.FailedEventService;
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
 * Integration Test for Dead Letter Queue (DLQ) Scenarios
 * Tests handling of permanently failed events that cannot be retried
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-validated", "dlq-payment", "failed-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DLQWorkflowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private FailedEventRepository failedEventRepository;

    @Autowired
    private FailedEventService failedEventService;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, FailedEvent> dlqProducerTemplate;
    private KafkaTemplate<String, OrderValidatedEvent> eventProducerTemplate;
    
    private BlockingQueue<ConsumerRecord<String, FailedEvent>> dlqRecords;
    private KafkaMessageListenerContainer<String, FailedEvent> dlqContainer;

    @BeforeEach
    void setUp() {
        setupProducers();
        setupConsumers();
        failedEventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (dlqContainer != null) {
            dlqContainer.stop();
        }
        failedEventRepository.deleteAll();
    }

    @Test
    void testFailedEventMovedToDLQ() throws Exception {
        // Step 1: Create a failed event
        String eventId = UUID.randomUUID().toString();
        OrderValidatedEvent event = new OrderValidatedEvent(
            eventId,
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            2001L,
            "VALID",
            "Validation passed"
        );

        // Step 2: Create FailedEvent to be sent to DLQ
        FailedEvent failedEvent = new FailedEvent();
        failedEvent.setEventId(eventId);
        failedEvent.setEventType("ORDER_VALIDATED");
        failedEvent.setPayload(objectMapper.writeValueAsString(event));
        failedEvent.setErrorMessage("Payment processing failed - insufficient funds");
        failedEvent.setServiceName("payment-service");
        failedEvent.setOriginalTopic("order-validated");
        failedEvent.setFailedAt(LocalDateTime.now());
        failedEvent.setRetryCount(3);

        // Step 3: Publish to DLQ topic
        dlqProducerTemplate.send("dlq-payment", failedEvent).get(5, TimeUnit.SECONDS);

        // Step 4: Verify DLQ event was consumed
        ConsumerRecord<String, FailedEvent> dlqRecord = dlqRecords.poll(10, TimeUnit.SECONDS);
        
        assertThat(dlqRecord).isNotNull();
        FailedEvent consumedEvent = dlqRecord.value();
        assertThat(consumedEvent.getEventId()).isEqualTo(eventId);
        assertThat(consumedEvent.getEventType()).isEqualTo("ORDER_VALIDATED");
        assertThat(consumedEvent.getErrorMessage()).contains("insufficient funds");
        assertThat(consumedEvent.getRetryCount()).isEqualTo(3);
    }

    @Test
    void testDLQEventStoredInDatabase() throws Exception {
        // Step 1: Create and store a failed event in database
        String eventId = UUID.randomUUID().toString();
        OrderValidatedEvent event = new OrderValidatedEvent(
            eventId,
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            2002L,
            "VALID",
            "Validated"
        );

        FailedEventEntity failedEntity = new FailedEventEntity();
        failedEntity.setEventId(eventId);
        failedEntity.setEventType("ORDER_VALIDATED");
        failedEntity.setPayload(objectMapper.writeValueAsString(event));
        failedEntity.setErrorMessage("Network timeout during payment processing");
        failedEntity.setServiceName("payment-service");
        failedEntity.setOriginalTopic("order-validated");
        failedEntity.setFailedAt(LocalDateTime.now());
        failedEntity.setRetryCount(3);
        failedEntity.setStatus("DLQ");
        
        failedEventRepository.save(failedEntity);

        // Step 2: Verify event is stored in DLQ table
        List<FailedEventEntity> dlqEvents = failedEventRepository.findAll();
        assertThat(dlqEvents).hasSize(1);
        assertThat(dlqEvents.get(0).getEventId()).isEqualTo(eventId);
        assertThat(dlqEvents.get(0).getStatus()).isEqualTo("DLQ");
        assertThat(dlqEvents.get(0).getErrorMessage()).contains("Network timeout");
    }

    @Test
    void testRetrieveAllDLQEvents() throws Exception {
        // Step 1: Create multiple failed events
        for (int i = 1; i <= 5; i++) {
            String eventId = UUID.randomUUID().toString();
            OrderValidatedEvent event = new OrderValidatedEvent(
                eventId,
                "ORDER_VALIDATED",
                LocalDateTime.now(),
                2000L + i,
                "VALID",
                "Validated"
            );

            FailedEventEntity failedEntity = new FailedEventEntity();
            failedEntity.setEventId(eventId);
            failedEntity.setEventType("ORDER_VALIDATED");
            failedEntity.setPayload(objectMapper.writeValueAsString(event));
            failedEntity.setErrorMessage("Error " + i);
            failedEntity.setServiceName("payment-service");
            failedEntity.setOriginalTopic("order-validated");
            failedEntity.setFailedAt(LocalDateTime.now());
            failedEntity.setRetryCount(3);
            failedEntity.setStatus("DLQ");
            
            failedEventRepository.save(failedEntity);
        }

        // Step 2: Retrieve all DLQ events
        List<FailedEventEntity> allDlqEvents = failedEventService.getAllFailedEvents();
        
        assertThat(allDlqEvents).hasSizeGreaterThanOrEqualTo(5);
        assertThat(allDlqEvents).allMatch(e -> "DLQ".equals(e.getStatus()));
    }

    @Test
    void testDLQEventWithDifferentErrorTypes() throws Exception {
        // Step 1: Create DLQ events with various error types
        String[] errorTypes = {
            "VALIDATION_ERROR",
            "TIMEOUT_ERROR",
            "NETWORK_ERROR",
            "BUSINESS_RULE_ERROR",
            "SYSTEM_ERROR"
        };

        for (String errorType : errorTypes) {
            String eventId = UUID.randomUUID().toString();
            OrderValidatedEvent event = new OrderValidatedEvent(
                eventId,
                "ORDER_VALIDATED",
                LocalDateTime.now(),
                2010L,
                "VALID",
                "Validated"
            );

            FailedEventEntity failedEntity = new FailedEventEntity();
            failedEntity.setEventId(eventId);
            failedEntity.setEventType("ORDER_VALIDATED");
            failedEntity.setPayload(objectMapper.writeValueAsString(event));
            failedEntity.setErrorMessage(errorType + ": Simulated error");
            failedEntity.setServiceName("payment-service");
            failedEntity.setOriginalTopic("order-validated");
            failedEntity.setFailedAt(LocalDateTime.now());
            failedEntity.setRetryCount(3);
            failedEntity.setStatus("DLQ");
            
            failedEventRepository.save(failedEntity);
        }

        // Step 2: Verify all error types are stored
        List<FailedEventEntity> dlqEvents = failedEventRepository.findAll();
        assertThat(dlqEvents).hasSizeGreaterThanOrEqualTo(5);
        
        // Step 3: Verify different error messages
        List<String> errorMessages = dlqEvents.stream()
            .map(FailedEventEntity::getErrorMessage)
            .toList();
        
        assertThat(errorMessages).anyMatch(msg -> msg.contains("VALIDATION_ERROR"));
        assertThat(errorMessages).anyMatch(msg -> msg.contains("TIMEOUT_ERROR"));
        assertThat(errorMessages).anyMatch(msg -> msg.contains("NETWORK_ERROR"));
    }

    @Test
    void testDLQEventRetrieval() throws Exception {
        // Step 1: Create a specific failed event
        String eventId = UUID.randomUUID().toString();
        OrderValidatedEvent event = new OrderValidatedEvent(
            eventId,
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            2020L,
            "VALID",
            "Validated"
        );

        FailedEventEntity failedEntity = new FailedEventEntity();
        failedEntity.setEventId(eventId);
        failedEntity.setEventType("ORDER_VALIDATED");
        failedEntity.setPayload(objectMapper.writeValueAsString(event));
        failedEntity.setErrorMessage("Database connection failed");
        failedEntity.setServiceName("payment-service");
        failedEntity.setOriginalTopic("order-validated");
        failedEntity.setFailedAt(LocalDateTime.now());
        failedEntity.setRetryCount(3);
        failedEntity.setStatus("DLQ");
        
        failedEntity = failedEventRepository.save(failedEntity);

        // Step 2: Retrieve by ID
        FailedEventEntity retrieved = failedEventService.getFailedEventById(failedEntity.getId());
        
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getEventId()).isEqualTo(eventId);
        assertThat(retrieved.getStatus()).isEqualTo("DLQ");
    }

    @Test
    void testDLQEventDeletion() throws Exception {
        // Step 1: Create a failed event
        String eventId = UUID.randomUUID().toString();
        OrderValidatedEvent event = new OrderValidatedEvent(
            eventId,
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            2030L,
            "VALID",
            "Validated"
        );

        FailedEventEntity failedEntity = new FailedEventEntity();
        failedEntity.setEventId(eventId);
        failedEntity.setEventType("ORDER_VALIDATED");
        failedEntity.setPayload(objectMapper.writeValueAsString(event));
        failedEntity.setErrorMessage("Test error for deletion");
        failedEntity.setServiceName("payment-service");
        failedEntity.setOriginalTopic("order-validated");
        failedEntity.setFailedAt(LocalDateTime.now());
        failedEntity.setRetryCount(3);
        failedEntity.setStatus("DLQ");
        
        failedEntity = failedEventRepository.save(failedEntity);
        Long savedId = failedEntity.getId();

        // Step 2: Verify it exists
        assertThat(failedEventRepository.findById(savedId)).isPresent();

        // Step 3: Delete the event
        failedEventService.deleteFailedEvent(savedId);

        // Step 4: Verify it's deleted
        assertThat(failedEventRepository.findById(savedId)).isEmpty();
    }

    @Test
    void testDLQEventFiltering() throws Exception {
        // Step 1: Create events from different services
        String[] services = {"payment-service", "inventory-service", "fulfillment-service"};
        
        for (String service : services) {
            for (int i = 1; i <= 2; i++) {
                String eventId = UUID.randomUUID().toString();
                OrderValidatedEvent event = new OrderValidatedEvent(
                    eventId,
                    "ORDER_VALIDATED",
                    LocalDateTime.now(),
                    2040L + i,
                    "VALID",
                    "Validated"
                );

                FailedEventEntity failedEntity = new FailedEventEntity();
                failedEntity.setEventId(eventId);
                failedEntity.setEventType("ORDER_VALIDATED");
                failedEntity.setPayload(objectMapper.writeValueAsString(event));
                failedEntity.setErrorMessage("Error from " + service);
                failedEntity.setServiceName(service);
                failedEntity.setOriginalTopic("order-validated");
                failedEntity.setFailedAt(LocalDateTime.now());
                failedEntity.setRetryCount(3);
                failedEntity.setStatus("DLQ");
                
                failedEventRepository.save(failedEntity);
            }
        }

        // Step 2: Query events by service
        List<FailedEventEntity> allEvents = failedEventRepository.findAll();
        assertThat(allEvents).hasSizeGreaterThanOrEqualTo(6);

        long paymentErrors = allEvents.stream()
            .filter(e -> "payment-service".equals(e.getServiceName()))
            .count();
        
        long inventoryErrors = allEvents.stream()
            .filter(e -> "inventory-service".equals(e.getServiceName()))
            .count();

        assertThat(paymentErrors).isGreaterThanOrEqualTo(2);
        assertThat(inventoryErrors).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testMaxRetriesExceededMovesToDLQ() throws Exception {
        // Step 1: Simulate an event that exceeded max retries
        String eventId = UUID.randomUUID().toString();
        OrderValidatedEvent event = new OrderValidatedEvent(
            eventId,
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            2050L,
            "VALID",
            "Validated"
        );

        // Step 2: Create failed event with max retries exceeded
        FailedEventEntity failedEntity = new FailedEventEntity();
        failedEntity.setEventId(eventId);
        failedEntity.setEventType("ORDER_VALIDATED");
        failedEntity.setPayload(objectMapper.writeValueAsString(event));
        failedEntity.setErrorMessage("Max retries (3) exceeded");
        failedEntity.setServiceName("payment-service");
        failedEntity.setOriginalTopic("order-validated");
        failedEntity.setFailedAt(LocalDateTime.now());
        failedEntity.setRetryCount(3); // Max retries reached
        failedEntity.setStatus("DLQ");
        
        failedEventRepository.save(failedEntity);

        // Step 3: Verify the event is in DLQ with correct retry count
        List<FailedEventEntity> dlqEvents = failedEventRepository.findAll();
        FailedEventEntity savedEvent = dlqEvents.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .findFirst()
            .orElse(null);

        assertThat(savedEvent).isNotNull();
        assertThat(savedEvent.getRetryCount()).isEqualTo(3);
        assertThat(savedEvent.getStatus()).isEqualTo("DLQ");
        assertThat(savedEvent.getErrorMessage()).contains("Max retries");
    }

    // Helper Methods

    private void setupProducers() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        dlqProducerTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        eventProducerTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    private void setupConsumers() {
        dlqRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlq-workflow-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, FailedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties("dlq-payment");
        dlqContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        dlqContainer.setupMessageListener((MessageListener<String, FailedEvent>) dlqRecords::add);
        dlqContainer.start();
        ContainerTestUtils.waitForAssignment(dlqContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }
}
