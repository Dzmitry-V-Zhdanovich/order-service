package com.example.orderservice.unit;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderMapper Unit Tests")
public class OrderMapperUnitTest {

    private OrderMapper orderMapper;
    private UUID testUserId;
    private UUID testItemId;
    private Item testItem;

    @BeforeEach
    void setUp() {
        orderMapper = Mappers.getMapper(OrderMapper.class);
        testUserId = UUID.randomUUID();
        testItemId = UUID.randomUUID();

        testItem = Item.builder()
                .id(testItemId)
                .name("Test Product")
                .price(BigDecimal.valueOf(75.25))
                .build();
    }

    @Test
    @DisplayName("Should map CreateOrderRequest to Order entity with Item relationship")
    void toEntity_ValidRequest_MapsCorrectly() {
        // Arrange
        CreateOrderRequest.OrderItemRequest itemRequest = CreateOrderRequest.OrderItemRequest.builder()
                .itemId(testItemId)
                .quantity(2)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(testUserId)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .orderItems(List.of(itemRequest))
                .build();

        // Act
        Order order = orderMapper.toEntity(request);

        // Assert
        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(testUserId);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("150.50");
        assertThat(order.getItems()).hasSize(1);

        assertThat(order.getItems().getFirst().getItem()).isNotNull();
        assertThat(order.getItems().getFirst().getItem().getId()).isEqualTo(testItemId);
        assertThat(order.getItems().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should map Order entity to OrderResponse with full item details")
    void toResponse_ValidOrder_MapsCorrectly() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        OrderItem orderItem = OrderItem.builder()
                .id(orderItemId)
                .item(testItem)
                .quantity(2)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .userId(testUserId)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .deleted(false)
                .items(List.of(orderItem))
                .build();

        // Act
        OrderResponse response = orderMapper.toResponse(order);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalPrice()).isEqualByComparingTo("150.50");
        assertThat(response.getDeleted()).isFalse();
        assertThat(response.getOrderItems()).hasSize(1);

        OrderResponse.OrderItemResponse responseItem = response.getOrderItems().getFirst();
        assertThat(responseItem.getId()).isEqualTo(orderItemId);
        assertThat(responseItem.getItemId()).isEqualTo(testItemId);
        assertThat(responseItem.getQuantity()).isEqualTo(2);
    }
}
