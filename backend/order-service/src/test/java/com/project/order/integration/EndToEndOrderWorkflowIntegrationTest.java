package com.project.order.integration;

import com.project.common.events.*;
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
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-End Integration Test for Complete Order Processing Workflow
 * Tests: Order → Validation → Payment → Inventory → Fulfillment
 * Verifies event publishing and consumption across all services
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "order-created",
        "order-validated",
        "payment-completed-events",
        "inventory-reserved-events",
        "order-completed"
    },
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EndToEndOrderWorkflowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderRepository orderRepository;

    private KafkaTemplate<String, OrderValidatedEvent> validationProducer;
    private KafkaTemplate<String, PaymentCompletedEvent> paymentProducer;
    private KafkaTemplate<String, InventoryReservedEvent> inventoryProducer;
    private KafkaTemplate<String, OrderCompletedEvent> fulfillmentProducer;

    private BlockingQueue<ConsumerRecord<String, OrderCreatedEvent>> orderCreatedRecords;
    private KafkaMessageListenerContainer<String, OrderCreatedEvent> orderCreatedContainer;

    @BeforeEach
    void setUp() {
        setupProducers();
        setupConsumers();
    }

    @AfterEach
    void tearDown() {
        if (orderCreatedContainer != null) {
            orderCreatedContainer.stop();
        }
        orderRepository.deleteAll();
    }

    @Test
    void testCompleteOrderToValidationWorkflow() throws Exception {
        // Step 1: Create Order via REST API
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("customerId", "CUST-001");
        orderRequest.put("totalAmount", 299.99);

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/orders",
            orderRequest,
            Map.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        
        Long orderId = ((Number) createResponse.getBody().get("id")).longValue();
        assertThat(orderId).isNotNull();

        // Step 2: Verify OrderCreatedEvent was published to Kafka
        ConsumerRecord<String, OrderCreatedEvent> orderCreatedRecord = 
            orderCreatedRecords.poll(10, TimeUnit.SECONDS);
        
        assertThat(orderCreatedRecord).isNotNull();
        OrderCreatedEvent orderCreatedEvent = orderCreatedRecord.value();
        assertThat(orderCreatedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(orderCreatedEvent.getCustomerId()).isEqualTo("CUST-001");
        assertThat(orderCreatedEvent.getTotalAmount()).isEqualByComparingTo(new BigDecimal("299.99"));
        assertThat(orderCreatedEvent.getOrderStatus()).isEqualTo("CREATED");

        // Step 3: Simulate Validation Service - Publish OrderValidatedEvent
        OrderValidatedEvent validatedEvent = new OrderValidatedEvent(
            UUID.randomUUID().toString(),
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            orderId,
            "VALID",
            "Order validation successful"
        );

        validationProducer.send("order-validated", validatedEvent).get(5, TimeUnit.SECONDS);

        // Step 4: Verify Order status updated to VALIDATED
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                OrderEntity order = orderRepository.findById(orderId).orElse(null);
                assertThat(order).isNotNull();
                assertThat(order.getStatus()).isEqualTo("VALIDATED");
            });
    }

    @Test
    void testValidationToPaymentWorkflow() throws Exception {
        // Setup: Create an order first
        OrderEntity order = createTestOrder("CUST-002", new BigDecimal("450.00"));
        Long orderId = order.getId();

        // Step 1: Publish OrderValidatedEvent
        OrderValidatedEvent validatedEvent = new OrderValidatedEvent(
            UUID.randomUUID().toString(),
            "ORDER_VALIDATED",
            LocalDateTime.now(),
            orderId,
            "VALID",
            "Validation passed"
        );

        validationProducer.send("order-validated", validatedEvent).get(5, TimeUnit.SECONDS);

        // Step 2: Wait for order status to update
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                OrderEntity updatedOrder = orderRepository.findById(orderId).orElse(null);
                assertThat(updatedOrder).isNotNull();
                assertThat(updatedOrder.getStatus()).isEqualTo("VALIDATED");
            });

        // Step 3: Simulate Payment Service - Publish PaymentCompletedEvent
        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            UUID.randomUUID().toString(),
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-" + orderId,
            orderId,
            new BigDecimal("450.00"),
            "COMPLETED",
            "PROD-001",
            "Test Product",
            2
        );

        paymentProducer.send("payment-completed-events", paymentEvent).get(5, TimeUnit.SECONDS);

        // Step 4: Verify Order status updated to PAYMENT_COMPLETED
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                OrderEntity updatedOrder = orderRepository.findById(orderId).orElse(null);
                assertThat(updatedOrder).isNotNull();
                assertThat(updatedOrder.getStatus()).isEqualTo("PAYMENT_COMPLETED");
            });
    }

    @Test
    void testPaymentToInventoryWorkflow() throws Exception {
        // Setup: Create order with payment completed status
        OrderEntity order = createTestOrder("CUST-003", new BigDecimal("599.99"));
        order.setStatus("PAYMENT_COMPLETED");
        order = orderRepository.save(order);
        Long orderId = order.getId();

        // Step 1: Publish PaymentCompletedEvent
        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            UUID.randomUUID().toString(),
            "PAYMENT_COMPLETED",
            LocalDateTime.now(),
            "PAY-" + orderId,
            orderId,
            new BigDecimal("599.99"),
            "COMPLETED",
            "PROD-002",
            "Premium Product",
            1
        );

        paymentProducer.send("payment-completed-events", paymentEvent).get(5, TimeUnit.SECONDS);

        // Wait for processing
        Thread.sleep(2000);

        // Step 2: Simulate Inventory Service - Publish InventoryReservedEvent
        InventoryReservedEvent inventoryEvent = new InventoryReservedEvent(
            UUID.randomUUID().toString(),
            "INVENTORY_RESERVED",
            LocalDateTime.now(),
            orderId,
            "PROD-002",
            1,
            "Inventory reserved successfully"
        );

        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);

        // Step 3: Verify Order status updated to INVENTORY_RESERVED
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                OrderEntity updatedOrder = orderRepository.findById(orderId).orElse(null);
                assertThat(updatedOrder).isNotNull();
                assertThat(updatedOrder.getStatus()).isEqualTo("INVENTORY_RESERVED");
            });
    }

    @Test
    void testInventoryToFulfillmentWorkflow() throws Exception {
        // Setup: Create order with inventory reserved status
        OrderEntity order = createTestOrder("CUST-004", new BigDecimal("199.99"));
        order.setStatus("INVENTORY_RESERVED");
        order = orderRepository.save(order);
        Long orderId = order.getId();

        // Step 1: Publish InventoryReservedEvent
        InventoryReservedEvent inventoryEvent = new InventoryReservedEvent(
            UUID.randomUUID().toString(),
            "INVENTORY_RESERVED",
            LocalDateTime.now(),
            orderId,
            "PROD-003",
            3,
            "Stock reserved"
        );

        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);

        // Wait for processing
        Thread.sleep(2000);

        // Step 2: Simulate Fulfillment Service - Publish OrderCompletedEvent
        OrderCompletedEvent completedEvent = new OrderCompletedEvent(
            UUID.randomUUID().toString(),
            "ORDER_COMPLETED",
            LocalDateTime.now(),
            orderId,
            "COMPLETED",
            "Order fulfilled successfully"
        );

        fulfillmentProducer.send("order-completed", completedEvent).get(5, TimeUnit.SECONDS);

        // Step 3: Verify Order status updated to COMPLETED
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                OrderEntity updatedOrder = orderRepository.findById(orderId).orElse(null);
                assertThat(updatedOrder).isNotNull();
                assertThat(updatedOrder.getStatus()).isEqualTo("COMPLETED");
            });
    }

    @Test
    void testCompleteEndToEndWorkflow() throws Exception {
        // Step 1: Create Order
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("customerId", "CUST-E2E-001");
        orderRequest.put("totalAmount", 999.99);

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/orders",
            orderRequest,
            Map.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long orderId = ((Number) createResponse.getBody().get("id")).longValue();

        // Step 2: Wait for OrderCreatedEvent
        ConsumerRecord<String, OrderCreatedEvent> orderCreatedRecord = 
            orderCreatedRecords.poll(10, TimeUnit.SECONDS);
        assertThat(orderCreatedRecord).isNotNull();
        assertThat(orderCreatedRecord.value().getOrderId()).isEqualTo(orderId);

        // Step 3: Simulate Validation → Payment → Inventory → Fulfillment
        // Validation
        OrderValidatedEvent validatedEvent = new OrderValidatedEvent(
            UUID.randomUUID().toString(), "ORDER_VALIDATED", LocalDateTime.now(),
            orderId, "VALID", "Validated"
        );
        validationProducer.send("order-validated", validatedEvent).get(5, TimeUnit.SECONDS);
        Thread.sleep(2000);

        // Payment
        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            UUID.randomUUID().toString(), "PAYMENT_COMPLETED", LocalDateTime.now(),
            "PAY-" + orderId, orderId, new BigDecimal("999.99"), "COMPLETED",
            "PROD-E2E", "E2E Product", 1
        );
        paymentProducer.send("payment-completed-events", paymentEvent).get(5, TimeUnit.SECONDS);
        Thread.sleep(2000);

        // Inventory
        InventoryReservedEvent inventoryEvent = new InventoryReservedEvent(
            UUID.randomUUID().toString(), "INVENTORY_RESERVED", LocalDateTime.now(),
            orderId, "PROD-E2E", 1, "Reserved"
        );
        inventoryProducer.send("inventory-reserved-events", inventoryEvent).get(5, TimeUnit.SECONDS);
        Thread.sleep(2000);

        // Fulfillment
        OrderCompletedEvent completedEvent = new OrderCompletedEvent(
            UUID.randomUUID().toString(), "ORDER_COMPLETED", LocalDateTime.now(),
            orderId, "COMPLETED", "Fulfilled"
        );
        fulfillmentProducer.send("order-completed", completedEvent).get(5, TimeUnit.SECONDS);

        // Step 4: Verify final status
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                OrderEntity finalOrder = orderRepository.findById(orderId).orElse(null);
                assertThat(finalOrder).isNotNull();
                assertThat(finalOrder.getStatus()).isEqualTo("COMPLETED");
            });
    }

    // Helper Methods

    private void setupProducers() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        validationProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        paymentProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        inventoryProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        fulfillmentProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    private void setupConsumers() {
        orderCreatedRecords = new LinkedBlockingQueue<>();
        orderCreatedContainer = setupContainer("order-created", OrderCreatedEvent.class, orderCreatedRecords);
    }

    private <T> KafkaMessageListenerContainer<String, T> setupContainer(
            String topic, Class<T> eventClass, BlockingQueue<ConsumerRecord<String, T>> records) {
        
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + topic);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, eventClass.getName());

        DefaultKafkaConsumerFactory<String, T> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties(topic);
        KafkaMessageListenerContainer<String, T> container = 
            new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        container.setupMessageListener((MessageListener<String, T>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        return container;
    }

    private OrderEntity createTestOrder(String customerId, BigDecimal amount) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(customerId);
        order.setTotalAmount(amount);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }
}
