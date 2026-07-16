package com.project.fulfillment.integration;

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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for Kafka Fulfillment Flow
 * Tests Inventory → Fulfillment flow (final step of the order processing)
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
class KafkaFulfillmentFlowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private FulfillmentRepository fulfillmentRepository;

    private KafkaTemplate<String, InventoryReservedEvent> producerTemplate;
    private BlockingQueue<ConsumerRecord<String, OrderCompletedEvent>> records;
    private KafkaMessageListenerContainer<String, OrderCompletedEvent> container;

    @BeforeEach
    void setUp() {
        // Setup producer to send InventoryReservedEvent
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, InventoryReservedEvent> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        producerTemplate = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify OrderCompletedEvent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-fulfillment-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, OrderCompletedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("order-completed");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, OrderCompletedEvent>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        if (fulfillmentRepository != null) {
            fulfillmentRepository.deleteAll();
        }
    }

    @Test
    void testFulfillmentFlow_ValidInventoryReserved_ShouldPublishOrderCompletedEvent() throws InterruptedException {
        // Arrange
        InventoryReservedEvent inventoryEvent = createInventoryReservedEvent(1L, "PROD-001", 2, 98, "RESERVED");

        // Act
        producerTemplate.send("inventory-reserved-events", "1", inventoryEvent);

        // Assert - Verify fulfillment processing and event published
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(fulfillmentRepository.findByOrderId(1L)).isPresent();
            });

        ConsumerRecord<String, OrderCompletedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("order-completed");
        
        OrderCompletedEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getFulfillmentStatus()).isEqualTo("COMPLETED");
        assertThat(event.getEventType()).isEqualTo("ORDER_COMPLETED");
        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    void testFulfillmentEventConsumption_ShouldVerifyKafkaConsumption() throws InterruptedException {
        // Arrange
        InventoryReservedEvent inventoryEvent = createInventoryReservedEvent(2L, "PROD-002", 5, 45, "RESERVED");

        // Act
        producerTemplate.send("inventory-reserved-events", "2", inventoryEvent);

        // Assert - Verify event was consumed and fulfillment was created
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                FulfillmentEntity fulfillment = fulfillmentRepository.findByOrderId(2L).orElse(null);
                assertThat(fulfillment).isNotNull();
                assertThat(fulfillment.getOrderId()).isEqualTo(2L);
                assertThat(fulfillment.getFulfillmentStatus()).isEqualTo("COMPLETED");
            });
    }

    @Test
    void testCompleteEventFlow_InventoryToFulfillment_ShouldMaintainEventChain() throws InterruptedException {
        // Arrange
        String originalEventId = UUID.randomUUID().toString();
        InventoryReservedEvent inventoryEvent = new InventoryReservedEvent();
        inventoryEvent.setEventId(originalEventId);
        inventoryEvent.setEventType("INVENTORY_RESERVED");
        inventoryEvent.setEventTimestamp(LocalDateTime.now());
        inventoryEvent.setProductId("PROD-003");
        inventoryEvent.setOrderId(3L);
        inventoryEvent.setAvailableQuantity(73);
        inventoryEvent.setReservedQuantity(2);
        inventoryEvent.setInventoryStatus("RESERVED");

        // Act
        producerTemplate.send("inventory-reserved-events", "3", inventoryEvent);

        // Assert - Verify complete event flow
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(fulfillmentRepository.findByOrderId(3L)).isPresent();
            });

        ConsumerRecord<String, OrderCompletedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        OrderCompletedEvent completedEvent = record.value();
        assertThat(completedEvent.getOrderId()).isEqualTo(3L);
        assertThat(completedEvent.getEventId()).isNotNull();
        assertThat(completedEvent.getEventTimestamp()).isAfter(inventoryEvent.getEventTimestamp().minusSeconds(1));
    }

    @Test
    void testFulfillmentFlow_MultipleOrders_ShouldProcessAllSuccessfully() throws InterruptedException {
        // Arrange
        InventoryReservedEvent event1 = createInventoryReservedEvent(4L, "PROD-001", 1, 99, "RESERVED");
        InventoryReservedEvent event2 = createInventoryReservedEvent(5L, "PROD-002", 3, 47, "RESERVED");
        InventoryReservedEvent event3 = createInventoryReservedEvent(6L, "PROD-003", 2, 73, "RESERVED");

        // Act
        producerTemplate.send("inventory-reserved-events", "4", event1);
        producerTemplate.send("inventory-reserved-events", "5", event2);
        producerTemplate.send("inventory-reserved-events", "6", event3);

        // Assert - Verify all fulfillments are processed
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(fulfillmentRepository.findByOrderId(4L)).isPresent();
                assertThat(fulfillmentRepository.findByOrderId(5L)).isPresent();
                assertThat(fulfillmentRepository.findByOrderId(6L)).isPresent();
            });

        ConsumerRecord<String, OrderCompletedEvent> record1 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, OrderCompletedEvent> record2 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, OrderCompletedEvent> record3 = records.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
        assertThat(record3).isNotNull();
    }

    @Test
    void testFulfillmentFlow_VerifyCustomerId_ShouldContainValidCustomerId() throws InterruptedException {
        // Arrange
        InventoryReservedEvent inventoryEvent = createInventoryReservedEvent(7L, "PROD-001", 1, 98, "RESERVED");

        // Act
        producerTemplate.send("inventory-reserved-events", "7", inventoryEvent);

        // Assert - Verify fulfillment contains customer ID
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                FulfillmentEntity fulfillment = fulfillmentRepository.findByOrderId(7L).orElse(null);
                assertThat(fulfillment).isNotNull();
                assertThat(fulfillment.getCustomerId()).isNotNull();
                assertThat(fulfillment.getCustomerId()).startsWith("CUST-");
            });
    }

    private InventoryReservedEvent createInventoryReservedEvent(Long orderId, String productId, 
                                                                int reservedQty, int availableQty, String status) {
        InventoryReservedEvent event = new InventoryReservedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("INVENTORY_RESERVED");
        event.setEventTimestamp(LocalDateTime.now());
        event.setProductId(productId);
        event.setOrderId(orderId);
        event.setAvailableQuantity(availableQty);
        event.setReservedQuantity(reservedQty);
        event.setInventoryStatus(status);
        return event;
    }
}
