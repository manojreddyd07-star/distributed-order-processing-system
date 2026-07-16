package com.project.payment.integration;

import com.project.common.events.OrderValidatedEvent;
import com.project.common.events.PaymentCompletedEvent;
import com.project.payment.entity.PaymentEntity;
import com.project.payment.repository.PaymentRepository;
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
 * Integration test for Kafka Payment Flow
 * Tests Validation → Payment → Inventory flow (third step of the order processing)
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-validated", "payment-completed-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaPaymentFlowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private PaymentRepository paymentRepository;

    private KafkaTemplate<String, OrderValidatedEvent> producerTemplate;
    private BlockingQueue<ConsumerRecord<String, PaymentCompletedEvent>> records;
    private KafkaMessageListenerContainer<String, PaymentCompletedEvent> container;

    @BeforeEach
    void setUp() {
        // Setup producer to send OrderValidatedEvent
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, OrderValidatedEvent> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        producerTemplate = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify PaymentCompletedEvent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-payment-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, PaymentCompletedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("payment-completed-events");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, PaymentCompletedEvent>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        if (paymentRepository != null) {
            paymentRepository.deleteAll();
        }
    }

    @Test
    void testPaymentToInventoryFlow_ValidOrder_ShouldPublishPaymentCompletedEvent() throws InterruptedException {
        // Arrange
        OrderValidatedEvent validatedEvent = createOrderValidatedEvent(1L, "VALID", "Order validation successful");

        // Act
        producerTemplate.send("order-validated", "1", validatedEvent);

        // Assert - Wait for payment processing and verify event published
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(paymentRepository.findByOrderId(1L)).isPresent();
            });

        ConsumerRecord<String, PaymentCompletedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("payment-completed-events");
        
        PaymentCompletedEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getPaymentStatus()).isEqualTo("COMPLETED");
        assertThat(event.getEventType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getPaymentId()).isNotNull();
        assertThat(event.getProductId()).isNotNull();
        assertThat(event.getProductName()).isNotNull();
        assertThat(event.getQuantity()).isGreaterThan(0);
    }

    @Test
    void testPaymentEventConsumption_ShouldVerifyKafkaConsumption() throws InterruptedException {
        // Arrange
        OrderValidatedEvent validatedEvent = createOrderValidatedEvent(2L, "VALID", "Order validation successful");

        // Act
        producerTemplate.send("order-validated", "2", validatedEvent);

        // Assert - Verify event was consumed and payment was stored
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                PaymentEntity payment = paymentRepository.findByOrderId(2L).orElse(null);
                assertThat(payment).isNotNull();
                assertThat(payment.getOrderId()).isEqualTo(2L);
                assertThat(payment.getPaymentStatus()).isEqualTo("COMPLETED");
            });
    }

    @Test
    void testPaymentFlow_InvalidValidationStatus_ShouldNotProcessPayment() throws InterruptedException {
        // Arrange - Order with INVALID validation status
        OrderValidatedEvent invalidEvent = createOrderValidatedEvent(3L, "INVALID", "Customer ID missing");

        // Act
        producerTemplate.send("order-validated", "3", invalidEvent);

        // Assert - Verify payment is not processed
        Thread.sleep(5000); // Wait to ensure no processing occurs
        assertThat(paymentRepository.findByOrderId(3L)).isEmpty();
        
        // Verify no PaymentCompletedEvent is published
        ConsumerRecord<String, PaymentCompletedEvent> record = records.poll(2, TimeUnit.SECONDS);
        // Either no record or record for a different order
        if (record != null) {
            assertThat(record.value().getOrderId()).isNotEqualTo(3L);
        }
    }

    @Test
    void testCompleteEventFlow_ValidationToPayment_ShouldMaintainEventChain() throws InterruptedException {
        // Arrange
        String originalEventId = UUID.randomUUID().toString();
        OrderValidatedEvent validatedEvent = new OrderValidatedEvent();
        validatedEvent.setEventId(originalEventId);
        validatedEvent.setEventType("ORDER_VALIDATED");
        validatedEvent.setEventTimestamp(LocalDateTime.now());
        validatedEvent.setOrderId(4L);
        validatedEvent.setValidationStatus("VALID");
        validatedEvent.setValidationMessage("Order validation successful");

        // Act
        producerTemplate.send("order-validated", "4", validatedEvent);

        // Assert - Verify complete event flow
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(paymentRepository.findByOrderId(4L)).isPresent();
            });

        ConsumerRecord<String, PaymentCompletedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        
        PaymentCompletedEvent paymentEvent = record.value();
        assertThat(paymentEvent.getOrderId()).isEqualTo(4L);
        assertThat(paymentEvent.getEventId()).isNotNull();
        assertThat(paymentEvent.getEventTimestamp()).isAfter(validatedEvent.getEventTimestamp().minusSeconds(1));
    }

    @Test
    void testPaymentFlow_MultipleOrders_ShouldProcessAllSuccessfully() throws InterruptedException {
        // Arrange
        OrderValidatedEvent event1 = createOrderValidatedEvent(5L, "VALID", "Validation successful");
        OrderValidatedEvent event2 = createOrderValidatedEvent(6L, "VALID", "Validation successful");
        OrderValidatedEvent event3 = createOrderValidatedEvent(7L, "VALID", "Validation successful");

        // Act
        producerTemplate.send("order-validated", "5", event1);
        producerTemplate.send("order-validated", "6", event2);
        producerTemplate.send("order-validated", "7", event3);

        // Assert - Verify all payments are processed
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(paymentRepository.findByOrderId(5L)).isPresent();
                assertThat(paymentRepository.findByOrderId(6L)).isPresent();
                assertThat(paymentRepository.findByOrderId(7L)).isPresent();
            });

        ConsumerRecord<String, PaymentCompletedEvent> record1 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, PaymentCompletedEvent> record2 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, PaymentCompletedEvent> record3 = records.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
        assertThat(record3).isNotNull();
    }

    private OrderValidatedEvent createOrderValidatedEvent(Long orderId, String status, String message) {
        OrderValidatedEvent event = new OrderValidatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("ORDER_VALIDATED");
        event.setEventTimestamp(LocalDateTime.now());
        event.setOrderId(orderId);
        event.setValidationStatus(status);
        event.setValidationMessage(message);
        return event;
    }
}
