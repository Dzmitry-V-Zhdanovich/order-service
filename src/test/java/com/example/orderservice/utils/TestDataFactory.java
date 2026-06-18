package com.example.orderservice.utils;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderRequest;
import com.example.orderservice.dto.response.UserInfoResponse;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestDataFactory {

    public static final UUID TEST_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    public static final UUID TEST_ITEM_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
    public static final UUID TEST_ORDER_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174002");

    public static UserInfoResponse createTestUser() {
        return UserInfoResponse.builder()
                .id(TEST_USER_ID)
                .email("john.doe@example.com")
                .name("John")
                .surname("Doe")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Item createTestItem() {
        return Item.builder()
                .id(TEST_ITEM_ID)
                .name("Test Product")
                .price(BigDecimal.valueOf(75.25))
                .build();
    }

    public static Order createTestOrder() {
        return Order.builder()
                .id(TEST_ORDER_ID)
                .userId(TEST_USER_ID)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .deleted(false)
                .items(new ArrayList<>())
                .build();
    }

    public static OrderItem createTestOrderItem(Order order, Item item) {
        return OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .item(item)
                .quantity(2)
                .build();
    }

    public static CreateOrderRequest createValidCreateOrderRequest() {
        CreateOrderRequest.OrderItemRequest orderItem = CreateOrderRequest.OrderItemRequest.builder()
                .itemId(TEST_ITEM_ID)
                .quantity(2)
                .build();

        return CreateOrderRequest.builder()
                .userId(TEST_USER_ID)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .orderItems(List.of(orderItem))
                .build();
    }

    public static UpdateOrderRequest createValidUpdateOrderRequest() {
        UpdateOrderRequest.OrderItemUpdateRequest orderItem =
                UpdateOrderRequest.OrderItemUpdateRequest.builder()
                        .id(null)
                        .itemId(TEST_ITEM_ID)
                        .quantity(3)
                        .build();

        return UpdateOrderRequest.builder()
                .status("CONFIRMED")
                .totalPrice(BigDecimal.valueOf(200.00))
                .orderItems(List.of(orderItem))
                .build();
    }
}
