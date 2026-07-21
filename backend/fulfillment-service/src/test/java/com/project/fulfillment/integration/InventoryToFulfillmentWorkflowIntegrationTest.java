package com.project.fulfillment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.InventoryReservedEvent;
import com.project.common.events.OrderCompletedEvent;
import com.project.fulfillment.entity.FulfillmentEntity;
import com.project.fulfillment.repository.FulfillmentRepository;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration Test: Inventory → Fulfillment Workflow
 * Tests event consumption from inventory service and order completion publishing
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"inventory-reserved-events", "order-completed"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventoryToFulfillmentWorkflowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private FulfillmentRepository fulfillmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, InventoryReservedEvent> inventoryProducer;
    private BlockingQueue<ConsumerRecord<String, OrderCompletedEvent>> orderCompletedRecords;
    private KafkaMessageListenerContainer<String, OrderCompletedEvent> orderCompletedContainer;

    @BeforeEach
    void setUp() {
        // Setup producer to send inventory reserved events
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, InventoryReservedEvent> producerFactory =
            new DefaultKafkaProducerFactory<>(producerProps);
        inventoryProducer = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify order completed events
        orderCompletedRecords = new LinkedBlockingQueue<>();
        orderCompletedContainer = setupConsumer("order-completed",
            OrderCompletedEvent.class, orderCompletedRecords);
    }

    @AfterEach
    void tearDown() {
        if (orderCompletedContainer != null) {
            orderCompletedContainer.stop();
        }
        fulfillmentRepository.deleteAll();
    }

    @Test
    void shouldProcessInventoryReservationAndCompleteFulfillment() throws Exception {
        // Given: Inventory reserved event
        InventoryReservedEvent inventoryEvent = InventoryReservedEvent.builder()
                .orderId(1L)
                .reservationId(1L)
                .status("RESERVED")
                .productId("PROD-001")
                .quantity(2)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Send inventory reserved event
        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);

        // Then: Fulfillment should be created in database
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            FulfillmentEntity fulfillment = fulfillmentRepository.findByOrderId(1L).orElse(null);
            assertThat(fulfillment).isNotNull();
            assertThat(fulfillment.getOrderId()).isEqualTo(1L);
            assertThat(fulfillment.getStatus()).isEqualTo("COMPLETED");
        });

        // And: Order completed event should be published
        ConsumerRecord<String, OrderCompletedEvent> record =
            orderCompletedRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value().getOrderId()).isEqualTo(1L);
        assertThat(record.value().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldHandleMultipleInventoryReservations() throws Exception {
        // Given: Multiple inventory reserved events
        InventoryReservedEvent event1 = InventoryReservedEvent.builder()
                .orderId(1L)
                .reservationId(1L)
                .status("RESERVED")
                .productId("PROD-001")
                .quantity(1)
                .timestamp(LocalDateTime.now())
                .build();

        InventoryReservedEvent event2 = InventoryReservedEvent.builder()
                .orderId(2L)
                .reservationId(2L)
                .status("RESERVED")
                .productId("PROD-002")
                .quantity(3)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Send both events
        inventoryProducer.send("inventory-reserved-events", event1).get(5, TimeUnit.SECONDS);
        inventoryProducer.send("inventory-reserved-events", event2).get(5, TimeUnit.SECONDS);

        // Then: Both fulfillments should be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(fulfillmentRepository.findByOrderId(1L)).isPresent();
            assertThat(fulfillmentRepository.findByOrderId(2L)).isPresent();
        });

        // And: Both order completed events should be published
        ConsumerRecord<String, OrderCompletedEvent> record1 =
            orderCompletedRecords.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, OrderCompletedEvent> record2 =
            orderCompletedRecords.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
    }

    @Test
    void shouldVerifyFulfillmentDetails() throws Exception {
        // Given: Inventory reservation
        InventoryReservedEvent inventoryEvent = InventoryReservedEvent.builder()
                .orderId(3L)
                .reservationId(3L)
                .status("RESERVED")
                .productId("PROD-003")
                .quantity(5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Process fulfillment
        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);

        // Then: Verify complete fulfillment details
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            FulfillmentEntity fulfillment = fulfillmentRepository.findByOrderId(3L).orElse(null);
            assertThat(fulfillment).isNotNull();
            assertThat(fulfillment.getOrderId()).isEqualTo(3L);
            assertThat(fulfillment.getStatus()).isEqualTo("COMPLETED");
            assertThat(fulfillment.getShippingAddress()).isNotNull();
            assertThat(fulfillment.getTrackingNumber()).isNotNull();
            assertThat(fulfillment.getCreatedAt()).isNotNull();
            assertThat(fulfillment.getCompletedAt()).isNotNull();
        });
    }

    @Test
    void shouldPublishOrderCompletedEventWithCorrectData() throws Exception {
        // Given: Inventory reserved
        InventoryReservedEvent inventoryEvent = InventoryReservedEvent.builder()
                .orderId(4L)
                .reservationId(4L)
                .status("RESERVED")
                .productId("PROD-004")
                .quantity(2)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Process fulfillment
        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);

        // Then: Order completed event should contain correct data
        ConsumerRecord<String, OrderCompletedEvent> record =
            orderCompletedRecords.poll(10, TimeUnit.SECONDS);
        
        assertThat(record).isNotNull();
        OrderCompletedEvent completedEvent = record.value();
        assertThat(completedEvent.getOrderId()).isEqualTo(4L);
        assertThat(completedEvent.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedEvent.getFulfillmentId()).isNotNull();
        assertThat(completedEvent.getTimestamp()).isNotNull();
    }

    @Test
    void shouldTrackFulfillmentTimestamps() throws Exception {
        // Given: Inventory reserved
        InventoryReservedEvent inventoryEvent = InventoryReservedEvent.builder()
                .orderId(5L)
                .reservationId(5L)
                .status("RESERVED")
                .productId("PROD-005")
                .quantity(1)
                .timestamp(LocalDateTime.now())
                .build();

        LocalDateTime beforeFulfillment = LocalDateTime.now();

        // When: Process fulfillment
        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);

        // Then: Timestamps should be tracked correctly
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            FulfillmentEntity fulfillment = fulfillmentRepository.findByOrderId(5L).orElse(null);
            assertThat(fulfillment).isNotNull();
            assertThat(fulfillment.getCreatedAt()).isNotNull();
            assertThat(fulfillment.getCompletedAt()).isNotNull();
            assertThat(fulfillment.getCreatedAt()).isAfterOrEqualTo(beforeFulfillment);
            assertThat(fulfillment.getCompletedAt()).isAfterOrEqualTo(fulfillment.getCreatedAt());
        });
    }

    private <T> KafkaMessageListenerContainer<String, T> setupConsumer(
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
