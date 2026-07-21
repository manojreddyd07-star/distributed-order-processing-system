package com.project.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.events.*;
import com.project.order.dto.OrderRequest;
import com.project.order.entity.OrderEntity;
import com.project.order.repository.OrderRepository;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Complete Order Lifecycle Integration Test
 * Tests the full end-to-end workflow with database verification:
 * Order → Validation → Payment → Inventory → Fulfillment
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "order-created",
        "order-validated",
        "payment-completed-events",
        "inventory-reserved-events",
        "order-completed",
        "order-failed"
    },
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CompleteOrderLifecycleIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, OrderValidatedEvent> validatedProducerTemplate;
    private KafkaTemplate<String, PaymentCompletedEvent> paymentProducerTemplate;
    private KafkaTemplate<String, InventoryReservedEvent> inventoryProducerTemplate;
    private KafkaTemplate<String, OrderCompletedEvent> completedProducerTemplate;

    private BlockingQueue<ConsumerRecord<String, OrderCreatedEvent>> orderCreatedRecords;
    private KafkaMessageListenerContainer<String, OrderCreatedEvent> orderCreatedContainer;

    @BeforeEach
    void setUp() {
        // Setup Kafka consumers
        orderCreatedRecords = new LinkedBlockingQueue<>();
        orderCreatedContainer = setupConsumer("order-created", OrderCreatedEvent.class, orderCreatedRecords);

        // Setup Kafka producers to simulate downstream services
        validatedProducerTemplate = createProducer();
        paymentProducerTemplate = createProducer();
        inventoryProducerTemplate = createProducer();
        completedProducerTemplate = createProducer();
    }

    @AfterEach
    void tearDown() {
        if (orderCreatedContainer != null) {
            orderCreatedContainer.stop();
        }
        orderRepository.deleteAll();
    }

    @Test
    void shouldCompleteFullOrderLifecycleSuccessfully() throws Exception {
        // Given: Create a new order via REST API
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomerId("CUST-LIFECYCLE-001");
        orderRequest.setTotalAmount(BigDecimal.valueOf(299.99));

        // When: Submit order
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/orders",
            orderRequest,
            Map.class
        );

        // Then: Order should be created
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long orderId = ((Number) createResponse.getBody().get("id")).longValue();
        assertThat(orderId).isNotNull();

        // Verify order-created event was published
        ConsumerRecord<String, OrderCreatedEvent> orderCreatedRecord = 
            orderCreatedRecords.poll(10, TimeUnit.SECONDS);
        assertThat(orderCreatedRecord).isNotNull();
        assertThat(orderCreatedRecord.value().getOrderId()).isEqualTo(orderId);
        assertThat(orderCreatedRecord.value().getCustomerId()).isEqualTo("CUST-LIFECYCLE-001");

        // Verify database: Order should be in CREATED status
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("CREATED");
            assertThat(orderInDb.getCustomerId()).isEqualTo("CUST-LIFECYCLE-001");
        });

        // Simulate: Validation service validates order
        OrderValidatedEvent validatedEvent = OrderValidatedEvent.builder()
                .orderId(orderId)
                .customerId("CUST-LIFECYCLE-001")
                .validationStatus("VALIDATED")
                .timestamp(LocalDateTime.now())
                .build();
        validatedProducerTemplate.send("order-validated", validatedEvent);

        // Verify database: Order status updated to VALIDATED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("VALIDATED");
        });

        // Simulate: Payment service completes payment
        PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .paymentId(1L)
                .amount(BigDecimal.valueOf(299.99))
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
        paymentProducerTemplate.send("payment-completed-events", paymentEvent);

        // Verify database: Order status updated to PAYMENT_COMPLETED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("PAYMENT_COMPLETED");
        });

        // Simulate: Inventory service reserves inventory
        InventoryReservedEvent inventoryEvent = InventoryReservedEvent.builder()
                .orderId(orderId)
                .reservationId(1L)
                .status("RESERVED")
                .timestamp(LocalDateTime.now())
                .build();
        inventoryProducerTemplate.send("inventory-reserved-events", inventoryEvent);

        // Verify database: Order status updated to INVENTORY_RESERVED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("INVENTORY_RESERVED");
        });

        // Simulate: Fulfillment service completes order
        OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                .orderId(orderId)
                .fulfillmentId(1L)
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
        completedProducerTemplate.send("order-completed", completedEvent);

        // Verify database: Order status updated to COMPLETED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("COMPLETED");
            assertThat(orderInDb.getCompletedAt()).isNotNull();
        });

        // Final verification: Retrieve order via REST API
        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId,
            Map.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().get("status")).isEqualTo("COMPLETED");
        assertThat(getResponse.getBody().get("customerId")).isEqualTo("CUST-LIFECYCLE-001");
    }

    @Test
    void shouldHandleOrderFailureInWorkflow() throws Exception {
        // Given: Create order
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomerId("CUST-LIFECYCLE-002");
        orderRequest.setTotalAmount(BigDecimal.valueOf(150.00));

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/orders",
            orderRequest,
            Map.class
        );

        Long orderId = ((Number) createResponse.getBody().get("id")).longValue();

        // Wait for order to be created
        orderCreatedRecords.poll(10, TimeUnit.SECONDS);

        // When: Simulate validation failure
        OrderValidatedEvent validatedEvent = OrderValidatedEvent.builder()
                .orderId(orderId)
                .customerId("CUST-LIFECYCLE-002")
                .validationStatus("FAILED")
                .validationErrors("Invalid customer data")
                .timestamp(LocalDateTime.now())
                .build();
        validatedProducerTemplate.send("order-validated", validatedEvent);

        // Then: Order should be marked as FAILED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("FAILED");
        });
    }

    @Test
    void shouldMaintainOrderHistoryThroughoutLifecycle() throws Exception {
        // Given: Create order
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomerId("CUST-LIFECYCLE-003");
        orderRequest.setTotalAmount(BigDecimal.valueOf(500.00));

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/orders",
            orderRequest,
            Map.class
        );

        Long orderId = ((Number) createResponse.getBody().get("id")).longValue();
        orderCreatedRecords.poll(10, TimeUnit.SECONDS);

        // Track timestamps
        LocalDateTime createdAt = LocalDateTime.now();

        // Progress through workflow
        OrderValidatedEvent validatedEvent = OrderValidatedEvent.builder()
                .orderId(orderId)
                .customerId("CUST-LIFECYCLE-003")
                .validationStatus("VALIDATED")
                .timestamp(LocalDateTime.now())
                .build();
        validatedProducerTemplate.send("order-validated", validatedEvent);
        Thread.sleep(1000);

        PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .paymentId(1L)
                .amount(BigDecimal.valueOf(500.00))
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
        paymentProducerTemplate.send("payment-completed-events", paymentEvent);
        Thread.sleep(1000);

        InventoryReservedEvent inventoryEvent = InventoryReservedEvent.builder()
                .orderId(orderId)
                .reservationId(1L)
                .status("RESERVED")
                .timestamp(LocalDateTime.now())
                .build();
        inventoryProducerTemplate.send("inventory-reserved-events", inventoryEvent);
        Thread.sleep(1000);

        OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                .orderId(orderId)
                .fulfillmentId(1L)
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
        completedProducerTemplate.send("order-completed", completedEvent);

        // Then: Verify complete order history
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity orderInDb = orderRepository.findById(orderId).orElse(null);
            assertThat(orderInDb).isNotNull();
            assertThat(orderInDb.getStatus()).isEqualTo("COMPLETED");
            assertThat(orderInDb.getCreatedAt()).isNotNull();
            assertThat(orderInDb.getUpdatedAt()).isNotNull();
            assertThat(orderInDb.getCompletedAt()).isNotNull();
            assertThat(orderInDb.getUpdatedAt()).isAfter(orderInDb.getCreatedAt());
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

    private <T> KafkaTemplate<String, T> createProducer() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, T> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        
        return new KafkaTemplate<>(producerFactory);
    }
}
