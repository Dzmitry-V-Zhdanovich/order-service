package com.example.orderservice.integration;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.OrderWithUserInfoResponse;
import com.example.orderservice.entity.Item;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.utils.TestDataFactory;
import com.example.orderservice.wiremock.UserServiceStubs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WireMockTest(httpPort = 8089)
@DisplayName("OrderController Integration Tests")
public class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_name")
            .withPassword("test_password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("user.service.url", () -> "http://localhost:8089");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID testOrderId;
    private static WireMockRuntimeInfo wireMockRuntimeInfo;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("POST /api/orders - Should create order successfully")
    void createOrder_Success() throws Exception {
        // Arrange
        Item testItem = Item.builder()
                .id(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"))
                .name("Test Product")
                .price(BigDecimal.valueOf(75.25))
                .build();
        entityManager.persist(testItem);
        entityManager.flush();
        entityManager.clear();

        CreateOrderRequest request = TestDataFactory.createValidCreateOrderRequest();
        UserServiceStubs.stubGetUserByIdSuccess(wireMockRuntimeInfo, TestDataFactory.TEST_USER_ID);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.order.userId").value(TestDataFactory.TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.order.status").value("PENDING"))
                .andExpect(jsonPath("$.order.totalPrice").value(150.50))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"))
                .andReturn();

        OrderWithUserInfoResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderWithUserInfoResponse.class);

        assertThat(response.getOrder().getId()).isNotNull();
        assertThat(response.getOrder().getUserId()).isEqualTo(TestDataFactory.TEST_USER_ID);
        assertThat(response.getUser().getName()).isEqualTo("John");

        assertThat(orderRepository.findById(response.getOrder().getId())).isPresent();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/orders - Should return 400 when validation fails")
    void createOrder_ValidationFailed() throws Exception {
        // Arrange
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .userId(null)
                .status("INVALID_STATUS")
                .totalPrice(BigDecimal.valueOf(-10))
                .orderItems(List.of())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists())
                .andExpect(jsonPath("$.validationErrors.userId").value("User ID cannot be null"))
                .andExpect(jsonPath("$.validationErrors.status").value("Status must be PENDING, CONFIRMED, SHIPPED, DELIVERED or CANCELLED"));
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("GET /api/orders/{id} - Should return order by id")
    void getOrderById_Success() throws Exception {
        // Arrange
        entityManager.createNativeQuery("""
            INSERT INTO orders (id, user_id, status, total_price, deleted, created_at, updated_at)
            VALUES (:id, :userId, :status, :totalPrice, :deleted, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
                .setParameter("id", testOrderId)
                .setParameter("userId", TestDataFactory.TEST_USER_ID)
                .setParameter("status", "PENDING")
                .setParameter("totalPrice", BigDecimal.valueOf(150.50))
                .setParameter("deleted", false)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        UserServiceStubs.stubGetUserByIdSuccess(wireMockRuntimeInfo, TestDataFactory.TEST_USER_ID);

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.order.userId").value(TestDataFactory.TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.order.status").value("PENDING"))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/orders/{id} - Should return 404 when order not found")
    void getOrderById_NotFound() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found with id: " + nonExistentId));
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("GET /api/orders/filter - Should return filtered orders with pagination")
    void getOrdersWithFilters_Success() throws Exception {
        // Arrange
        UUID id1 = UUID.fromString("323e4567-e89b-12d3-a456-426614174002");
        UUID id2 = UUID.randomUUID();

        entityManager.createNativeQuery("""
            INSERT INTO orders (id, user_id, status, total_price, deleted, created_at, updated_at)
            VALUES (:id, :userId, :status, :totalPrice, :deleted, :createdAt, CURRENT_TIMESTAMP)
            """)
                .setParameter("id", id1)
                .setParameter("userId", TestDataFactory.TEST_USER_ID)
                .setParameter("status", "PENDING")
                .setParameter("totalPrice", BigDecimal.valueOf(150.50))
                .setParameter("deleted", false)
                .setParameter("createdAt", java.time.LocalDateTime.now().minusDays(2))
                .executeUpdate();

        entityManager.createNativeQuery("""
            INSERT INTO orders (id, user_id, status, total_price, deleted, created_at, updated_at) 
            VALUES (:id, :userId, :status, :totalPrice, :deleted, :createdAt, CURRENT_TIMESTAMP)
            """)
                .setParameter("id", id2)
                .setParameter("userId", TestDataFactory.TEST_USER_ID)
                .setParameter("status", "CONFIRMED")
                .setParameter("totalPrice", BigDecimal.valueOf(100.00))
                .setParameter("deleted", false)
                .setParameter("createdAt", java.time.LocalDateTime.now().minusDays(1))
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        UserServiceStubs.stubGetUserByIdSuccess(wireMockRuntimeInfo, TestDataFactory.TEST_USER_ID);

        // Act & Assert
        mockMvc.perform(get("/api/orders/filter")
                        .param("statuses", "PENDING", "CONFIRMED")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }
}
