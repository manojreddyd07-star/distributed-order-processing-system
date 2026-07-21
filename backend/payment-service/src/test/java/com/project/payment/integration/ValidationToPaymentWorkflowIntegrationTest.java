package com.project.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration Test: Validation → Payment Workflow
 * Tests event consumption from validation service and payment completion publishing
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
class ValidationToPaymentWorkflowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, OrderValidatedEvent> validatedProducer;
    private BlockingQueue<ConsumerRecord<String, PaymentCompletedEvent>> paymentCompletedRecords;
    private KafkaMessageListenerContainer<String, PaymentCompletedEvent> paymentCompletedContainer;

    @BeforeEach
    void setUp() {
        // Setup producer to send validated events
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, OrderValidatedEvent> producerFactory =
            new DefaultKafkaProducerFactory<>(producerProps);
        validatedProducer = new KafkaTemplate<>(producerFactory);

        // Setup consumer to verify payment completed events
        paymentCompletedRecords = new LinkedBlockingQueue<>();
        paymentCompletedContainer = setupConsumer("payment-completed-events", 
            PaymentCompletedEvent.class, paymentCompletedRecords);
    }

    @AfterEach
    void tearDown() {
        if (paymentCompletedContainer != null) {
            paymentCompletedContainer.stop();
        }
        paymentRepository.deleteAll();
    }

    @Test
    void shouldProcessValidatedOrderAndCompletePayment() throws Exception {
        // Given: Order validated event
        OrderValidatedEvent validatedEvent = OrderValidatedEvent.builder()
                .orderId(1L)
                .customerId("CUST-PAY-001")
                .validationStatus("VALIDATED")
                .totalAmount(BigDecimal.valueOf(199.99))
                .timestamp(LocalDateTime.now())
                .build();

        // When: Send validated event
        validatedProducer.send("order-validated", validatedEvent).get(5, TimeUnit.SECONDS);

        // Then: Payment should be created in database
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            PaymentEntity payment = paymentRepository.findByOrderId(1L).orElse(null);
            assertThat(payment).isNotNull();
            assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199.99));
            assertThat(payment.getStatus()).isEqualTo("COMPLETED");
        });

        // And: Payment completed event should be published
        ConsumerRecord<String, PaymentCompletedEvent> record = 
            paymentCompletedRecords.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value().getOrderId()).isEqualTo(1L);
        assertThat(record.value().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199.99));
        assertThat(record.value().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldHandleMultipleValidatedOrders() throws Exception {
        // Given: Multiple validated orders
        OrderValidatedEvent event1 = OrderValidatedEvent.builder()
                .orderId(1L)
                .customerId("CUST-PAY-002")
                .validationStatus("VALIDATED")
                .totalAmount(BigDecimal.valueOf(100.00))
                .timestamp(LocalDateTime.now())
                .build();

        OrderValidatedEvent event2 = OrderValidatedEvent.builder()
                .orderId(2L)
                .customerId("CUST-PAY-003")
                .validationStatus("VALIDATED")
                .totalAmount(BigDecimal.valueOf(200.00))
                .timestamp(LocalDateTime.now())
                .build();

        // When: Send both events
        validatedProducer.send("order-validated", event1).get(5, TimeUnit.SECONDS);
        validatedProducer.send("order-validated", event2).get(5, TimeUnit.SECONDS);

        // Then: Both payments should be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(paymentRepository.findByOrderId(1L)).isPresent();
            assertThat(paymentRepository.findByOrderId(2L)).isPresent();
        });

        // And: Both payment completed events should be published
        ConsumerRecord<String, PaymentCompletedEvent> record1 = 
            paymentCompletedRecords.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, PaymentCompletedEvent> record2 = 
            paymentCompletedRecords.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
    }

    @Test
    void shouldUpdateDatabaseWithPaymentDetails() throws Exception {
        // Given: Validated order
        OrderValidatedEvent validatedEvent = OrderValidatedEvent.builder()
                .orderId(3L)
                .customerId("CUST-PAY-004")
                .validationStatus("VALIDATED")
                .totalAmount(BigDecimal.valueOf(350.00))
                .timestamp(LocalDateTime.now())
                .build();

        // When: Process payment
        validatedProducer.send("order-validated", validatedEvent).get(5, TimeUnit.SECONDS);

        // Then: Database should contain complete payment record
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            PaymentEntity payment = paymentRepository.findByOrderId(3L).orElse(null);
            assertThat(payment).isNotNull();
            assertThat(payment.getOrderId()).isEqualTo(3L);
            assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(350.00));
            assertThat(payment.getStatus()).isEqualTo("COMPLETED");
            assertThat(payment.getPaymentMethod()).isNotNull();
            assertThat(payment.getCreatedAt()).isNotNull();
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
