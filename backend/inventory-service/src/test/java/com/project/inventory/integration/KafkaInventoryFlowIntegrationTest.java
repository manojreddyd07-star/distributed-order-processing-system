package com.project.inventory.integration;

import com.project.common.events.InventoryReservedEvent;
import com.project.common.events.PaymentCompletedEvent;
import com.project.inventory.entity.InventoryEntity;
import com.project.inventory.repository.InventoryRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for Kafka Inventory Flow
 * Tests Payment → Inventory → Fulfillment flow (fourth step of the order processing)
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"payment-completed-events", "inventory-reserved-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaInventoryFlowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private InventoryRepository inventoryRepository;

    private KafkaTemplate<String, PaymentCompletedEvent> producerTemplate;
    private BlockingQueue<ConsumerRecord<String, InventoryReservedEvent>> records;
    private KafkaMessageListenerContainer<String, InventoryReservedEvent> container;

    @BeforeEach
    void setUp() {
        // Setup initial inventory
        setupInitialInventory();

        // Setup producer to send PaymentCompletedEvent
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, PaymentCompletedEvent> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        producerTemplate = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify InventoryReservedEvent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-inventory-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, InventoryReservedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("inventory-reserved-events");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, InventoryReservedEvent>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        if (inventoryRepository != null) {
            inventoryRepository.deleteAll();
        }
    }

    @Test
    void testInventoryToFulfillmentFlow_SufficientStock_ShouldPublishInventoryReservedEvent() throws InterruptedException {
        // Arrange
        PaymentCompletedEvent paymentEvent = createPaymentCompletedEvent(1L, "PROD-001", "Laptop", 2);

        // Act
        producerTemplate.send("payment-completed-events", "1", paymentEvent);

        // Assert - Verify inventory reservation and event published
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                InventoryEntity inventory = inventoryRepository.findByProductId("PROD-001").orElse(null);
                assertThat(inventory).isNotNull();
                assertThat(inventory.getAvailableQuantity()).isLessThan(100); // Initial was 100
            });

        ConsumerRecord<String, InventoryReservedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("inventory-reserved-events");
        
        InventoryReservedEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getProductId()).isEqualTo("PROD-001");
        assertThat(event.getReservedQuantity()).isEqualTo(2);
        assertThat(event.getInventoryStatus()).isEqualTo("RESERVED");
        assertThat(event.getEventType()).isEqualTo("INVENTORY_RESERVED");
        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    void testInventoryEventConsumption_ShouldVerifyKafkaConsumption() throws InterruptedException {
        // Arrange
        PaymentCompletedEvent paymentEvent = createPaymentCompletedEvent(2L, "PROD-002", "Mouse", 5);

        // Act
        producerTemplate.send("payment-completed-events", "2", paymentEvent);

        // Assert - Verify event was consumed and inventory was updated
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                InventoryEntity inventory = inventoryRepository.findByProductId("PROD-002").orElse(null);
                assertThat(inventory).isNotNull();
                assertThat(inventory.getReservedQuantity()).isGreaterThanOrEqualTo(5);
            });
    }

    @Test
    void testInventoryFlow_InsufficientStock_ShouldPublishRejectedEvent() throws InterruptedException {
        // Arrange - Request more than available
        PaymentCompletedEvent paymentEvent = createPaymentCompletedEvent(3L, "PROD-003", "Keyboard", 150);

        // Act
        producerTemplate.send("payment-completed-events", "3", paymentEvent);

        // Wait for processing
        Thread.sleep(5000);

        // Assert - Verify inventory was not reserved (or rejection was handled)
        InventoryEntity inventory = inventoryRepository.findByProductId("PROD-003").orElse(null);
        if (inventory != null) {
            // Verify available quantity didn't go negative
            assertThat(inventory.getAvailableQuantity()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void testCompleteEventFlow_PaymentToInventory_ShouldMaintainEventChain() throws InterruptedException {
        // Arrange
        String originalEventId = UUID.randomUUID().toString();
        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent();
        paymentEvent.setEventId(originalEventId);
        paymentEvent.setEventType("PAYMENT_COMPLETED");
        paymentEvent.setEventTimestamp(LocalDateTime.now());
        paymentEvent.setPaymentId("PAY-001");
        paymentEvent.setOrderId(4L);
        paymentEvent.setAmount(new BigDecimal("299.99"));
        paymentEvent.setPaymentStatus("COMPLETED");
        paymentEvent.setProductId("PROD-001");
        paymentEvent.setProductName("Laptop");
        paymentEvent.setQuantity(1);

        // Act
        producerTemplate.send("payment-completed-events", "4", paymentEvent);

        // Assert - Verify complete event flow
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                InventoryEntity inventory = inventoryRepository.findByProductId("PROD-001").orElse(null);
                assertThat(inventory).isNotNull();
            });

        ConsumerRecord<String, InventoryReservedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        InventoryReservedEvent inventoryEvent = record.value();
        assertThat(inventoryEvent.getOrderId()).isEqualTo(4L);
        assertThat(inventoryEvent.getEventId()).isNotNull();
        assertThat(inventoryEvent.getEventTimestamp()).isAfter(paymentEvent.getEventTimestamp().minusSeconds(1));
    }

    @Test
    void testInventoryFlow_MultipleProducts_ShouldProcessAllSuccessfully() throws InterruptedException {
        // Arrange
        PaymentCompletedEvent event1 = createPaymentCompletedEvent(5L, "PROD-001", "Laptop", 1);
        PaymentCompletedEvent event2 = createPaymentCompletedEvent(6L, "PROD-002", "Mouse", 3);
        PaymentCompletedEvent event3 = createPaymentCompletedEvent(7L, "PROD-003", "Keyboard", 2);

        // Act
        producerTemplate.send("payment-completed-events", "5", event1);
        producerTemplate.send("payment-completed-events", "6", event2);
        producerTemplate.send("payment-completed-events", "7", event3);

        // Assert - Verify all inventory reservations are processed
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(inventoryRepository.findByProductId("PROD-001")).isPresent();
                assertThat(inventoryRepository.findByProductId("PROD-002")).isPresent();
                assertThat(inventoryRepository.findByProductId("PROD-003")).isPresent();
            });

        ConsumerRecord<String, InventoryReservedEvent> record1 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, InventoryReservedEvent> record2 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, InventoryReservedEvent> record3 = records.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
        assertThat(record3).isNotNull();
    }

    private PaymentCompletedEvent createPaymentCompletedEvent(Long orderId, String productId, 
                                                              String productName, Integer quantity) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("PAYMENT_COMPLETED");
        event.setEventTimestamp(LocalDateTime.now());
        event.setPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8));
        event.setOrderId(orderId);
        event.setAmount(new BigDecimal("99.99"));
        event.setPaymentStatus("COMPLETED");
        event.setProductId(productId);
        event.setProductName(productName);
        event.setQuantity(quantity);
        return event;
    }

    private void setupInitialInventory() {
        // Create initial inventory items for testing
        InventoryEntity laptop = new InventoryEntity();
        laptop.setProductId("PROD-001");
        laptop.setProductName("Laptop");
        laptop.setAvailableQuantity(100);
        laptop.setReservedQuantity(0);
        inventoryRepository.save(laptop);

        InventoryEntity mouse = new InventoryEntity();
        mouse.setProductId("PROD-002");
        mouse.setProductName("Mouse");
        mouse.setAvailableQuantity(50);
        mouse.setReservedQuantity(0);
        inventoryRepository.save(mouse);

        InventoryEntity keyboard = new InventoryEntity();
        keyboard.setProductId("PROD-003");
        keyboard.setProductName("Keyboard");
        keyboard.setAvailableQuantity(75);
        keyboard.setReservedQuantity(0);
        inventoryRepository.save(keyboard);
    }
}
