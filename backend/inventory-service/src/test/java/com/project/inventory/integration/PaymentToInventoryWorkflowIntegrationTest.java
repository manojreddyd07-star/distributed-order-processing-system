package com.project.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.InventoryReservedEvent;
import com.project.common.events.PaymentCompletedEvent;
import com.project.inventory.entity.InventoryEntity;
import com.project.inventory.entity.ReservationEntity;
import com.project.inventory.repository.InventoryRepository;
import com.project.inventory.repository.ReservationRepository;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration Test: Payment → Inventory Workflow
 * Tests event consumption from payment service and inventory reservation publishing
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
class PaymentToInventoryWorkflowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, PaymentCompletedEvent> paymentProducer;
    private BlockingQueue<ConsumerRecord<String, InventoryReservedEvent>> inventoryReservedRecords;
    private KafkaMessageListenerContainer<String, InventoryReservedEvent> inventoryReservedContainer;

    @BeforeEach
    void setUp() {
        // Setup producer to send payment completed events
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, PaymentCompletedEvent> producerFactory =
            new DefaultKafkaProducerFactory<>(producerProps);
        paymentProducer = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify inventory reserved events
        inventoryReservedRecords = new LinkedBlockingQueue<>();
        inventoryReservedContainer = setupConsumer("inventory-reserved-events",
            InventoryReservedEvent.class, inventoryReservedRecords);

        // Initialize some inventory
        InventoryEntity inventory = new InventoryEntity();
        inventory.setProductId("PROD-001");
        inventory.setQuantity(100);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);
    }

    @AfterEach
    void tearDown() {
        if (inventoryReservedContainer != null) {
            inventoryReservedContainer.stop();
        }
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    void shouldProcessPaymentAndReserveInventory() throws Exception {
        // Given: Payment completed event
        PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                .orderId(1L)
                .paymentId(1L)
                .amount(BigDecimal.valueOf(299.99))
                .status("COMPLETED")
                .productId("PROD-001")
                .quantity(2)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Send payment completed event
        paymentProducer.send("payment-completed-events", paymentEvent).get(5, TimeUnit.SECONDS);

        // Then: Reservation should be created in database
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ReservationEntity reservation = reservationRepository.findByOrderId(1L).orElse(null);
            assertThat(reservation).isNotNull();
            assertThat(reservation.getOrderId()).isEqualTo(1L);
            assertThat(reservation.getStatus()).isEqualTo("RESERVED");
        });

        // And: Inventory should be updated
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryEntity inventory = inventoryRepository.findByProductId("PROD-001").orElse(null);
            assertThat(inventory).isNotNull();
            assertThat(inventory.getReservedQuantity()).isGreaterThan(0);
        });

        // And: Inventory reserved event should be published
        ConsumerRecord<String, InventoryReservedEvent> record =
            inventoryReservedRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value().getOrderId()).isEqualTo(1L);
        assertThat(record.value().getStatus()).isEqualTo("RESERVED");
    }

    @Test
    void shouldHandleMultiplePaymentCompletions() throws Exception {
        // Given: Multiple payment completed events
        PaymentCompletedEvent event1 = PaymentCompletedEvent.builder()
                .orderId(1L)
                .paymentId(1L)
                .amount(BigDecimal.valueOf(100.00))
                .status("COMPLETED")
                .productId("PROD-001")
                .quantity(1)
                .timestamp(LocalDateTime.now())
                .build();

        PaymentCompletedEvent event2 = PaymentCompletedEvent.builder()
                .orderId(2L)
                .paymentId(2L)
                .amount(BigDecimal.valueOf(200.00))
                .status("COMPLETED")
                .productId("PROD-001")
                .quantity(2)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Send both events
        paymentProducer.send("payment-completed-events", event1).get(5, TimeUnit.SECONDS);
        paymentProducer.send("payment-completed-events", event2).get(5, TimeUnit.SECONDS);

        // Then: Both reservations should be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(reservationRepository.findByOrderId(1L)).isPresent();
            assertThat(reservationRepository.findByOrderId(2L)).isPresent();
        });

        // And: Both inventory reserved events should be published
        ConsumerRecord<String, InventoryReservedEvent> record1 =
            inventoryReservedRecords.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, InventoryReservedEvent> record2 =
            inventoryReservedRecords.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
    }

    @Test
    void shouldUpdateInventoryQuantityCorrectly() throws Exception {
        // Given: Initial inventory
        InventoryEntity inventory = inventoryRepository.findByProductId("PROD-001").orElseThrow();
        int initialQuantity = inventory.getQuantity();
        int initialReserved = inventory.getReservedQuantity();

        // When: Reserve inventory
        PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                .orderId(3L)
                .paymentId(3L)
                .amount(BigDecimal.valueOf(150.00))
                .status("COMPLETED")
                .productId("PROD-001")
                .quantity(5)
                .timestamp(LocalDateTime.now())
                .build();

        paymentProducer.send("payment-completed-events", paymentEvent).get(5, TimeUnit.SECONDS);

        // Then: Reserved quantity should increase
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryEntity updatedInventory = inventoryRepository.findByProductId("PROD-001").orElseThrow();
            assertThat(updatedInventory.getReservedQuantity()).isEqualTo(initialReserved + 5);
            assertThat(updatedInventory.getQuantity()).isEqualTo(initialQuantity); // Total quantity unchanged
        });
    }

    @Test
    void shouldVerifyReservationDetails() throws Exception {
        // Given: Payment completed
        PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                .orderId(4L)
                .paymentId(4L)
                .amount(BigDecimal.valueOf(250.00))
                .status("COMPLETED")
                .productId("PROD-001")
                .quantity(3)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Process reservation
        paymentProducer.send("payment-completed-events", paymentEvent).get(5, TimeUnit.SECONDS);

        // Then: Verify complete reservation details
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ReservationEntity reservation = reservationRepository.findByOrderId(4L).orElse(null);
            assertThat(reservation).isNotNull();
            assertThat(reservation.getOrderId()).isEqualTo(4L);
            assertThat(reservation.getStatus()).isEqualTo("RESERVED");
            assertThat(reservation.getCreatedAt()).isNotNull();
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
