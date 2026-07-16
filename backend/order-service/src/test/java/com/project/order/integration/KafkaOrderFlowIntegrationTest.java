package com.project.order.integration;

import com.project.common.events.OrderCreatedEvent;
import com.project.order.entity.OrderEntity;
import com.project.order.repository.OrderRepository;
import com.project.order.service.KafkaProducerService;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Kafka Order Flow
 * Tests Order → Validation flow (first step of the order processing)
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaOrderFlowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private OrderRepository orderRepository;

    private BlockingQueue<ConsumerRecord<String, OrderCreatedEvent>> records;
    private KafkaMessageListenerContainer<String, OrderCreatedEvent> container;

    @BeforeEach
    void setUp() {
        // Setup consumer to verify messages
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, OrderCreatedEvent> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("order-created");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, OrderCreatedEvent>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        if (orderRepository != null) {
            orderRepository.deleteAll();
        }
    }

    @Test
    void testOrderToValidationFlow_ShouldPublishOrderCreatedEvent() throws InterruptedException {
        // Arrange
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setCustomerId("CUST-001");
        order.setStatus("CREATED");
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setCreatedAt(LocalDateTime.now());

        // Act
        kafkaProducerService.publishOrderCreatedEvent(order);

        // Assert - Verify Kafka message publishing
        ConsumerRecord<String, OrderCreatedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("order-created");
        
        OrderCreatedEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getCustomerId()).isEqualTo("CUST-001");
        assertThat(event.getOrderStatus()).isEqualTo("CREATED");
        assertThat(event.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventTimestamp()).isNotNull();
    }

    @Test
    void testOrderToValidationFlow_WithMultipleOrders_ShouldPublishAllEvents() throws InterruptedException {
        // Arrange
        OrderEntity order1 = createOrder(1L, "CUST-001", new BigDecimal("50.00"));
        OrderEntity order2 = createOrder(2L, "CUST-002", new BigDecimal("100.00"));
        OrderEntity order3 = createOrder(3L, "CUST-003", new BigDecimal("150.00"));

        // Act
        kafkaProducerService.publishOrderCreatedEvent(order1);
        kafkaProducerService.publishOrderCreatedEvent(order2);
        kafkaProducerService.publishOrderCreatedEvent(order3);

        // Assert - Verify all three messages were published
        ConsumerRecord<String, OrderCreatedEvent> record1 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, OrderCreatedEvent> record2 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, OrderCreatedEvent> record3 = records.poll(10, TimeUnit.SECONDS);

        assertThat(record1).isNotNull();
        assertThat(record2).isNotNull();
        assertThat(record3).isNotNull();

        assertThat(record1.value().getOrderId()).isEqualTo(1L);
        assertThat(record2.value().getOrderId()).isEqualTo(2L);
        assertThat(record3.value().getOrderId()).isEqualTo(3L);
    }

    @Test
    void testKafkaMessageKey_ShouldUseOrderId() throws InterruptedException {
        // Arrange
        OrderEntity order = createOrder(123L, "CUST-123", new BigDecimal("75.50"));

        // Act
        kafkaProducerService.publishOrderCreatedEvent(order);

        // Assert - Verify message key is order ID
        ConsumerRecord<String, OrderCreatedEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("123");
    }

    private OrderEntity createOrder(Long id, String customerId, BigDecimal amount) {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setCustomerId(customerId);
        order.setStatus("CREATED");
        order.setTotalAmount(amount);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
