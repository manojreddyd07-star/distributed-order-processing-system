package com.project.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.PaymentCompletedEvent;
import com.project.inventory.entity.ReplayEventEntity;
import com.project.inventory.repository.ReplayEventRepository;
import com.project.inventory.service.EventReplayService;
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
 * Integration Test for Event Replay Scenarios
 * Tests replaying events from DLQ or historical data
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"payment-completed-events", "replay-events", "inventory-reserved-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReplayWorkflowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ReplayEventRepository replayEventRepository;

    @Autowired
    private EventReplayService eventReplayService;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, PaymentCompletedEvent> paymentProducerTemplate;
    
    private BlockingQueue<ConsumerRecord<String, PaymentCompletedEvent>> paymentRecords;
    private KafkaMessageListenerContainer<String, PaymentCompletedEvent> paymentContainer;

    @BeforeEach
    void setUp() {
        setupProducers();
        setupConsumers();
        replayEventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (paymentContainer != null) {
            paymentContainer.stop();
        }
        replayEventRepository.deleteAll();
    }

    @Test
    void testStoreEventForReplay() throws Exception {
        // Step 1: Create an event to be stored for potential replay
        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3001",
            3001L,
            new BigDecimal("299.99"),
            "COMPLETED",
            "PROD-REPLAY-001",
            "Replay Test Product",
            2
        );

        // Step 2: Store event for potential replay
        ReplayEventEntity replayEntity = new ReplayEventEntity();
        replayEntity.setEventId(eventId);
        replayEntity.setEventType("PAYMENT_COMPLETED");
        replayEntity.setPayload(objectMapper.writeValueAsString(event));
        replayEntity.setServiceName("inventory-service");
        replayEntity.setOriginalTopic("payment-completed-events");
        replayEntity.setCreatedAt(LocalDateTime.now());
        replayEntity.setStatus("STORED");
        
        replayEventRepository.save(replayEntity);

        // Step 3: Verify event is stored
        List<ReplayEventEntity> storedEvents = replayEventRepository.findAll();
        assertThat(storedEvents).hasSize(1);
        assertThat(storedEvents.get(0).getEventId()).isEqualTo(eventId);
        assertThat(storedEvents.get(0).getStatus()).isEqualTo("STORED");
    }

    @Test
    void testReplayStoredEvent() throws Exception {
        // Step 1: Create and store an event for replay
        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3002",
            3002L,
            new BigDecimal("450.00"),
            "COMPLETED",
            "PROD-REPLAY-002",
            "Product for Replay",
            1
        );

        ReplayEventEntity replayEntity = new ReplayEventEntity();
        replayEntity.setEventId(eventId);
        replayEntity.setEventType("PAYMENT_COMPLETED");
        replayEntity.setPayload(objectMapper.writeValueAsString(event));
        replayEntity.setServiceName("inventory-service");
        replayEntity.setOriginalTopic("payment-completed-events");
        replayEntity.setCreatedAt(LocalDateTime.now());
        replayEntity.setStatus("STORED");
        
        replayEntity = replayEventRepository.save(replayEntity);
        Long replayId = replayEntity.getId();

        // Step 2: Trigger replay
        eventReplayService.replayEvent(replayId);

        // Step 3: Verify event status updated to REPLAYED
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ReplayEventEntity updated = replayEventRepository.findById(replayId).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getStatus()).isEqualTo("REPLAYED");
                assertThat(updated.getReplayedAt()).isNotNull();
            });
    }

    @Test
    void testReplayMultipleEvents() throws Exception {
        // Step 1: Create multiple events for replay
        for (int i = 1; i <= 5; i++) {
            String eventId = UUID.randomUUID().toString();
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId,
                "PAYMENT_COMPLETED",
                LocalDateTime.now(),
                "PAY-300" + (10 + i),
                3010L + i,
                new BigDecimal("100.00").multiply(new BigDecimal(i)),
                "COMPLETED",
                "PROD-" + i,
                "Product " + i,
                1
            );

            ReplayEventEntity replayEntity = new ReplayEventEntity();
            replayEntity.setEventId(eventId);
            replayEntity.setEventType("PAYMENT_COMPLETED");
            replayEntity.setPayload(objectMapper.writeValueAsString(event));
            replayEntity.setServiceName("inventory-service");
            replayEntity.setOriginalTopic("payment-completed-events");
            replayEntity.setCreatedAt(LocalDateTime.now());
            replayEntity.setStatus("STORED");
            
            replayEventRepository.save(replayEntity);
        }

        // Step 2: Get all stored events
        List<ReplayEventEntity> storedEvents = replayEventRepository.findAll();
        assertThat(storedEvents).hasSizeGreaterThanOrEqualTo(5);

        // Step 3: Replay all events
        for (ReplayEventEntity entity : storedEvents) {
            eventReplayService.replayEvent(entity.getId());
        }

        // Step 4: Verify all events were replayed
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<ReplayEventEntity> allEvents = replayEventRepository.findAll();
                long replayedCount = allEvents.stream()
                    .filter(e -> "REPLAYED".equals(e.getStatus()))
                    .count();
                assertThat(replayedCount).isGreaterThanOrEqualTo(5);
            });
    }

    @Test
    void testReplayEventPublishedToKafka() throws Exception {
        // Step 1: Create an event to replay
        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3020",
            3020L,
            new BigDecimal("599.99"),
            "COMPLETED",
            "PROD-REPLAY-KAFKA",
            "Kafka Replay Product",
            3
        );

        // Step 2: Publish event to Kafka directly (simulating replay)
        paymentProducerTemplate.send("payment-completed-events", event).get(5, TimeUnit.SECONDS);

        // Step 3: Verify event was consumed from Kafka
        ConsumerRecord<String, PaymentCompletedEvent> paymentRecord = 
            paymentRecords.poll(10, TimeUnit.SECONDS);
        
        assertThat(paymentRecord).isNotNull();
        PaymentCompletedEvent consumedEvent = paymentRecord.value();
        assertThat(consumedEvent.getEventId()).isEqualTo(eventId);
        assertThat(consumedEvent.getPaymentId()).isEqualTo("PAY-3020");
        assertThat(consumedEvent.getOrderId()).isEqualTo(3020L);
    }

    @Test
    void testReplayFailedEventFromDLQ() throws Exception {
        // Step 1: Create an event that was previously in DLQ
        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3030",
            3030L,
            new BigDecimal("750.00"),
            "COMPLETED",
            "PROD-DLQ-REPLAY",
            "DLQ Replay Product",
            2
        );

        ReplayEventEntity replayEntity = new ReplayEventEntity();
        replayEntity.setEventId(eventId);
        replayEntity.setEventType("PAYMENT_COMPLETED");
        replayEntity.setPayload(objectMapper.writeValueAsString(event));
        replayEntity.setServiceName("inventory-service");
        replayEntity.setOriginalTopic("payment-completed-events");
        replayEntity.setCreatedAt(LocalDateTime.now());
        replayEntity.setStatus("FROM_DLQ"); // Indicates this came from DLQ
        
        replayEntity = replayEventRepository.save(replayEntity);

        // Step 2: Replay the DLQ event
        eventReplayService.replayEvent(replayEntity.getId());

        // Step 3: Verify event was replayed successfully
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ReplayEventEntity updated = replayEventRepository.findById(replayEntity.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getStatus()).isEqualTo("REPLAYED");
            });
    }

    @Test
    void testBulkReplayByDateRange() throws Exception {
        // Step 1: Create events with different timestamps
        LocalDateTime baseTime = LocalDateTime.now().minusDays(5);
        
        for (int i = 0; i < 10; i++) {
            String eventId = UUID.randomUUID().toString();
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId,
                "PAYMENT_COMPLETED",
                baseTime.plusDays(i),
                "PAY-304" + i,
                3040L + i,
                new BigDecimal("200.00"),
                "COMPLETED",
                "PROD-DATE-" + i,
                "Date Range Product " + i,
                1
            );

            ReplayEventEntity replayEntity = new ReplayEventEntity();
            replayEntity.setEventId(eventId);
            replayEntity.setEventType("PAYMENT_COMPLETED");
            replayEntity.setPayload(objectMapper.writeValueAsString(event));
            replayEntity.setServiceName("inventory-service");
            replayEntity.setOriginalTopic("payment-completed-events");
            replayEntity.setCreatedAt(baseTime.plusDays(i));
            replayEntity.setStatus("STORED");
            
            replayEventRepository.save(replayEntity);
        }

        // Step 2: Query events within specific date range
        LocalDateTime startDate = baseTime.plusDays(2);
        LocalDateTime endDate = baseTime.plusDays(7);
        
        List<ReplayEventEntity> eventsInRange = replayEventRepository.findAll().stream()
            .filter(e -> !e.getCreatedAt().isBefore(startDate) && !e.getCreatedAt().isAfter(endDate))
            .toList();

        // Step 3: Verify correct number of events in range
        assertThat(eventsInRange).hasSizeGreaterThanOrEqualTo(5);

        // Step 4: Replay events in date range
        for (ReplayEventEntity entity : eventsInRange) {
            eventReplayService.replayEvent(entity.getId());
        }

        // Step 5: Verify replayed
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                long replayedInRange = replayEventRepository.findAll().stream()
                    .filter(e -> !e.getCreatedAt().isBefore(startDate) && !e.getCreatedAt().isAfter(endDate))
                    .filter(e -> "REPLAYED".equals(e.getStatus()))
                    .count();
                assertThat(replayedInRange).isGreaterThanOrEqualTo(5);
            });
    }

    @Test
    void testReplayEventWithUpdatedData() throws Exception {
        // Step 1: Create original event
        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent originalEvent = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3050",
            3050L,
            new BigDecimal("300.00"),
            "COMPLETED",
            "PROD-UPDATED",
            "Original Product",
            1
        );

        ReplayEventEntity replayEntity = new ReplayEventEntity();
        replayEntity.setEventId(eventId);
        replayEntity.setEventType("PAYMENT_COMPLETED");
        replayEntity.setPayload(objectMapper.writeValueAsString(originalEvent));
        replayEntity.setServiceName("inventory-service");
        replayEntity.setOriginalTopic("payment-completed-events");
        replayEntity.setCreatedAt(LocalDateTime.now());
        replayEntity.setStatus("STORED");
        
        replayEntity = replayEventRepository.save(replayEntity);

        // Step 2: Update event payload before replay (simulating corrected data)
        PaymentCompletedEvent updatedEvent = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3050",
            3050L,
            new BigDecimal("350.00"), // Updated amount
            "COMPLETED",
            "PROD-UPDATED",
            "Updated Product Name", // Updated name
            2 // Updated quantity
        );

        replayEntity.setPayload(objectMapper.writeValueAsString(updatedEvent));
        replayEntity = replayEventRepository.save(replayEntity);

        // Step 3: Replay the updated event
        eventReplayService.replayEvent(replayEntity.getId());

        // Step 4: Verify event was replayed with updated data
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ReplayEventEntity updated = replayEventRepository.findById(replayEntity.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getStatus()).isEqualTo("REPLAYED");
                
                // Parse and verify updated payload
                PaymentCompletedEvent replayedEvent = objectMapper.readValue(
                    updated.getPayload(), 
                    PaymentCompletedEvent.class
                );
                assertThat(replayedEvent.getAmount()).isEqualByComparingTo(new BigDecimal("350.00"));
                assertThat(replayedEvent.getProductName()).isEqualTo("Updated Product Name");
                assertThat(replayedEvent.getQuantity()).isEqualTo(2);
            });
    }

    @Test
    void testReplayEventStatusTracking() throws Exception {
        // Step 1: Create event with STORED status
        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            eventId,
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-3060",
            3060L,
            new BigDecimal("400.00"),
            "COMPLETED",
            "PROD-STATUS",
            "Status Tracking Product",
            1
        );

        ReplayEventEntity replayEntity = new ReplayEventEntity();
        replayEntity.setEventId(eventId);
        replayEntity.setEventType("PAYMENT_COMPLETED");
        replayEntity.setPayload(objectMapper.writeValueAsString(event));
        replayEntity.setServiceName("inventory-service");
        replayEntity.setOriginalTopic("payment-completed-events");
        replayEntity.setCreatedAt(LocalDateTime.now());
        replayEntity.setStatus("STORED");
        
        replayEntity = replayEventRepository.save(replayEntity);
        assertThat(replayEntity.getStatus()).isEqualTo("STORED");

        // Step 2: Replay event
        eventReplayService.replayEvent(replayEntity.getId());

        // Step 3: Verify status transitions
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ReplayEventEntity updated = replayEventRepository.findById(replayEntity.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getStatus()).isEqualTo("REPLAYED");
                assertThat(updated.getReplayedAt()).isNotNull();
                assertThat(updated.getReplayedAt()).isAfter(updated.getCreatedAt());
            });
    }

    // Helper Methods

    private void setupProducers() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        paymentProducerTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    private void setupConsumers() {
        paymentRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-replay-workflow-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, PaymentCompletedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties("payment-completed-events");
        paymentContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        paymentContainer.setupMessageListener((MessageListener<String, PaymentCompletedEvent>) paymentRecords::add);
        paymentContainer.start();
        ContainerTestUtils.waitForAssignment(paymentContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }
}
