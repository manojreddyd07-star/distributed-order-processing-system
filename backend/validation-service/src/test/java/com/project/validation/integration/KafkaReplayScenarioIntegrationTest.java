package com.project.validation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.OrderCreatedEvent;
import com.project.validation.dto.ReplayRequest;
import com.project.validation.dto.ReplayResponse;
import com.project.validation.entity.FailedEventEntity;
import com.project.validation.repository.FailedEventRepository;
import com.project.validation.service.EventReplayService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for Kafka Replay Scenarios
 * Tests event replay mechanism for failed events from DLQ
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created", "order-validated", "dlq-orders"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaReplayScenarioIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private FailedEventRepository failedEventRepository;

    @Autowired
    private EventReplayService eventReplayService;

    @Autowired
    private ObjectMapper objectMapper;

    private BlockingQueue<ConsumerRecord<String, OrderCreatedEvent>> orderCreatedRecords;
    private KafkaMessageListenerContainer<String, OrderCreatedEvent> orderCreatedContainer;

    @BeforeEach
    void setUp() {
        // Setup consumer to verify replayed events
        orderCreatedRecords = new LinkedBlockingQueue<>();
        orderCreatedContainer = setupContainer("order-created", OrderCreatedEvent.class, orderCreatedRecords);
    }

    @AfterEach
    void tearDown() {
        if (orderCreatedContainer != null) {
            orderCreatedContainer.stop();
        }
        failedEventRepository.deleteAll();
    }

    @Test
    void shouldReplayFailedEventSuccessfully() throws Exception {
        // Given: A failed event stored in database
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .customerId("CUST-REPLAY-001")
                .totalAmount(BigDecimal.valueOf(150.00))
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        String eventData = objectMapper.writeValueAsString(orderCreatedEvent);
        
        FailedEventEntity failedEvent = new FailedEventEntity();
        failedEvent.setEventId(eventId);
        failedEvent.setEventType("OrderCreatedEvent");
        failedEvent.setEventData(eventData);
        failedEvent.setTopic("order-created");
        failedEvent.setErrorMessage("Simulated failure");
        failedEvent.setRetryCount(3);
        failedEvent.setFailedAt(LocalDateTime.now());
        
        failedEventRepository.save(failedEvent);

        // When: Replay the failed event
        ReplayRequest replayRequest = new ReplayRequest();
        replayRequest.setEventId(eventId);
        replayRequest.setTargetTopic("order-created");
        
        ReplayResponse response = eventReplayService.replayEvent(replayRequest);

        // Then: Event should be successfully replayed
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getReplayTopic()).isEqualTo("order-created");

        // Verify event was published to Kafka
        ConsumerRecord<String, OrderCreatedEvent> record = orderCreatedRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value().getCustomerId()).isEqualTo("CUST-REPLAY-001");
        assertThat(record.value().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
    }

    @Test
    void shouldReplayMultipleFailedEvents() throws Exception {
        // Given: Multiple failed events
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();

        OrderCreatedEvent event1 = OrderCreatedEvent.builder()
                .orderId(1L)
                .customerId("CUST-REPLAY-002")
                .totalAmount(BigDecimal.valueOf(100.00))
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        OrderCreatedEvent event2 = OrderCreatedEvent.builder()
                .orderId(2L)
                .customerId("CUST-REPLAY-003")
                .totalAmount(BigDecimal.valueOf(200.00))
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        saveFailedEvent(eventId1, event1);
        saveFailedEvent(eventId2, event2);

        // When: Replay both events
        ReplayRequest request1 = new ReplayRequest();
        request1.setEventId(eventId1);
        request1.setTargetTopic("order-created");
        
        ReplayRequest request2 = new ReplayRequest();
        request2.setEventId(eventId2);
        request2.setTargetTopic("order-created");

        ReplayResponse response1 = eventReplayService.replayEvent(request1);
        ReplayResponse response2 = eventReplayService.replayEvent(request2);

        // Then: Both events should be replayed
        assertThat(response1.isSuccess()).isTrue();
        assertThat(response2.isSuccess()).isTrue();

        // Verify both events were published
        ConsumerRecord<String, OrderCreatedEvent> record1 = orderCreatedRecords.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, OrderCreatedEvent> record2 = orderCreatedRecords.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
    }

    @Test
    void shouldHandleReplayOfNonExistentEvent() {
        // Given: An event ID that doesn't exist
        String nonExistentEventId = UUID.randomUUID().toString();
        
        ReplayRequest request = new ReplayRequest();
        request.setEventId(nonExistentEventId);
        request.setTargetTopic("order-created");

        // When: Try to replay non-existent event
        ReplayResponse response = eventReplayService.replayEvent(request);

        // Then: Should return error response
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("not found");
    }

    @Test
    void shouldTrackReplayTimestamp() throws Exception {
        // Given: A failed event
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(1L)
                .customerId("CUST-REPLAY-004")
                .totalAmount(BigDecimal.valueOf(99.99))
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        saveFailedEvent(eventId, event);

        // When: Replay the event
        ReplayRequest request = new ReplayRequest();
        request.setEventId(eventId);
        request.setTargetTopic("order-created");
        
        LocalDateTime beforeReplay = LocalDateTime.now();
        eventReplayService.replayEvent(request);

        // Then: Failed event should be marked as replayed with timestamp
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            FailedEventEntity failedEvent = failedEventRepository.findByEventId(eventId).orElse(null);
            assertThat(failedEvent).isNotNull();
            assertThat(failedEvent.getReplayedAt()).isNotNull();
            assertThat(failedEvent.getReplayedAt()).isAfterOrEqualTo(beforeReplay);
        });
    }

    private void saveFailedEvent(String eventId, OrderCreatedEvent event) throws Exception {
        String eventData = objectMapper.writeValueAsString(event);
        
        FailedEventEntity failedEvent = new FailedEventEntity();
        failedEvent.setEventId(eventId);
        failedEvent.setEventType("OrderCreatedEvent");
        failedEvent.setEventData(eventData);
        failedEvent.setTopic("order-created");
        failedEvent.setErrorMessage("Test failure");
        failedEvent.setRetryCount(3);
        failedEvent.setFailedAt(LocalDateTime.now());
        
        failedEventRepository.save(failedEvent);
    }

    private <T> KafkaMessageListenerContainer<String, T> setupContainer(
            String topic, 
            Class<T> targetType,
            BlockingQueue<ConsumerRecord<String, T>> records) {
        
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + topic);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());

        DefaultKafkaConsumerFactory<String, T> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(topic);
        KafkaMessageListenerContainer<String, T> container = 
            new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        container.setupMessageListener((MessageListener<String, T>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        return container;
    }
}
