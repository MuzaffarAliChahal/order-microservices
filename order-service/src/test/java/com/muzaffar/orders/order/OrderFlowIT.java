package com.muzaffar.orders.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzaffar.orders.events.OrderCreatedEvent;
import com.muzaffar.orders.events.PaymentEvent;
import com.muzaffar.orders.events.Topics;
import com.muzaffar.orders.order.domain.OrderRepository;
import com.muzaffar.orders.order.domain.OrderStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of order-service against an embedded Kafka broker:
 * REST call -> order saved -> OrderCreatedEvent published -> PaymentEvent consumed -> order confirmed/cancelled.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {Topics.ORDERS, Topics.PAYMENTS})
class OrderFlowIT {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private OrderRepository orders;
    @Autowired private KafkaTemplate<String, String> kafka;
    @Autowired private EmbeddedKafkaBroker broker;

    private Consumer<String, String> ordersConsumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ordersConsumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromAnEmbeddedTopic(ordersConsumer, Topics.ORDERS);
    }

    @AfterEach
    void tearDown() {
        ordersConsumer.close();
    }

    private long placeOrder(String customerId, String unitPrice) throws Exception {
        String body = """
                {"customerId":"%s","product":"Mechanical keyboard","quantity":2,"unitPrice":%s}
                """.formatted(customerId, unitPrice);
        String response = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private OrderCreatedEvent nextOrderEvent(long orderId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(ordersConsumer, Duration.ofSeconds(2))) {
                OrderCreatedEvent event = json.readValue(record.value(), OrderCreatedEvent.class);
                if (event.orderId() == orderId) {
                    return event;
                }
            }
        }
        throw new AssertionError("No OrderCreatedEvent for order " + orderId);
    }

    @Test
    void publishesEventAndConfirmsOnSuccessfulPayment() throws Exception {
        long id = placeOrder("customer-1", "60.00");

        OrderCreatedEvent event = nextOrderEvent(id);
        assertThat(event.total()).isEqualByComparingTo("120.00");
        assertThat(event.customerId()).isEqualTo("customer-1");

        var payment = new PaymentEvent(id, "customer-1", event.total(), true, null, Instant.now());
        kafka.send(Topics.PAYMENTS, String.valueOf(id), json.writeValueAsString(payment));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED));
    }

    @Test
    void cancelsOnFailedPayment() throws Exception {
        long id = placeOrder("customer-2", "900.00");
        nextOrderEvent(id);

        var payment = new PaymentEvent(id, "customer-2", new BigDecimal("1800.00"), false,
                "amount exceeds limit", Instant.now());
        kafka.send(Topics.PAYMENTS, String.valueOf(id), json.writeValueAsString(payment));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var order = orders.findById(id).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getFailureReason()).isEqualTo("amount exceeds limit");
        });
    }

    @Test
    void rejectsInvalidOrders() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"product\":\"x\",\"quantity\":0,\"unitPrice\":-1}"))
                .andExpect(status().isBadRequest());
    }
}
