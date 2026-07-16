package com.project.validation.integration;

import com.project.common.events.OrderCreatedEvent;
import com.project.common.events.OrderValidatedEvent;
import com.project.validation.entity.ValidationEntity;
import com.project.validation.repository.ValidationRepository;
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
 * Integration test for Kafka Validation Flow
 * Tests Validation → Payment flow (second step of the order processing)
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created", "order-validated"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaValidationFlowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ValidationRepository validationRepository;

    private KafkaTemplate<String, OrderCreatedEvent> producerTemplate;
    private BlockingQueue<ConsumerRecord<String, OrderValidatedEvent>> records;
    private KafkaMessageListenerContainer<String, OrderValidatedEvent> container;

    @BeforeEach
    void setUp() {
        // Setup producer to send OrderCreatedEvent
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, OrderCreatedEvent> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        producerTemplate = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify OrderValidatedEvent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-validation-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, OrderValidatedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("order-validated");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, OrderValidatedEvent>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        if (validationRepository != null) {
            validationRepository.deleteAll();
        }
    }

    @Test
    void testValidationToPaymentFlow_ValidOrder_ShouldPublishOrderValidatedEvent() throws InterruptedException {
        // Arrange
        OrderCreatedEvent orderEvent = createOrderCreatedEvent(1L, "CUST-001", new BigDecimal("99.99"));

        // Act
        producerTemplate.send("order-created", "1", orderEvent);

        // Assert - Wait for validation processing and verify event published
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(validationRepository.findByOrderId(1L)).isPresent();
            });

        ConsumerRecord<String, OrderValidatedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("order-validated");
        
        OrderValidatedEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getValidationStatus()).isEqualTo("VALID");
        assertThat(event.getValidationMessage()).isEqualTo("Order validation successful");
        assertThat(event.getEventType()).isEqualTo("ORDER_VALIDATED");
        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    void testValidationToPaymentFlow_InvalidOrder_ShouldPublishFailedValidationEvent() throws InterruptedException {
        // Arrange - Order with null customerId (invalid)
        OrderCreatedEvent orderEvent = new OrderCreatedEvent();
        orderEvent.setEventId(UUID.randomUUID().toString());
        orderEvent.setEventType("ORDER_CREATED");
        orderEvent.setEventTimestamp(LocalDateTime.now());
        orderEvent.setOrderId(2L);
        orderEvent.setCustomerId(null); // Invalid
        orderEvent.setOrderStatus("CREATED");
        orderEvent.setTotalAmount(new BigDecimal("50.00"));

        // Act
        producerTemplate.send("order-created", "2", orderEvent);

        // Assert - Verify validation fails and event is published
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ValidationEntity validation = validationRepository.findByOrderId(2L).orElse(null);
                assertThat(validation).isNotNull();
                assertThat(validation.getValidationStatus()).isEqualTo("INVALID");
            });

        ConsumerRecord<String, OrderValidatedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        OrderValidatedEvent event = record.value();
        assertThat(event.getOrderId()).isEqualTo(2L);
        assertThat(event.getValidationStatus()).isEqualTo("INVALID");
        assertThat(event.getValidationMessage()).contains("Customer ID is required");
    }

    @Test
    void testValidationEventConsumption_ShouldVerifyKafkaConsumption() throws InterruptedException {
        // Arrange
        OrderCreatedEvent orderEvent = createOrderCreatedEvent(3L, "CUST-003", new BigDecimal("150.00"));

        // Act
        producerTemplate.send("order-created", "3", orderEvent);

        // Assert - Verify event was consumed and stored in database
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ValidationEntity validation = validationRepository.findByOrderId(3L).orElse(null);
                assertThat(validation).isNotNull();
                assertThat(validation.getOrderId()).isEqualTo(3L);
                assertThat(validation.getValidationStatus()).isEqualTo("VALID");
            });
    }

    @Test
    void testCompleteEventFlow_OrderToValidation_ShouldMaintainEventChain() throws InterruptedException {
        // Arrange
        String originalEventId = UUID.randomUUID().toString();
        OrderCreatedEvent orderEvent = new OrderCreatedEvent();
        orderEvent.setEventId(originalEventId);
        orderEvent.setEventType("ORDER_CREATED");
        orderEvent.setEventTimestamp(LocalDateTime.now());
        orderEvent.setOrderId(4L);
        orderEvent.setCustomerId("CUST-004");
        orderEvent.setOrderStatus("CREATED");
        orderEvent.setTotalAmount(new BigDecimal("200.00"));

        // Act
        producerTemplate.send("order-created", "4", orderEvent);

        // Assert - Verify complete event flow
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(validationRepository.findByOrderId(4L)).isPresent();
            });

        ConsumerRecord<String, OrderValidatedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        OrderValidatedEvent validatedEvent = record.value();
        assertThat(validatedEvent.getOrderId()).isEqualTo(4L);
        assertThat(validatedEvent.getEventId()).isNotNull();
        assertThat(validatedEvent.getEventTimestamp()).isAfter(orderEvent.getEventTimestamp().minusSeconds(1));
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
}
