package com.example.orderservice.service;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderRequest;
import com.example.orderservice.dto.response.OrderWithUserInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderWithUserInfoResponse createOrder(CreateOrderRequest request);

    OrderWithUserInfoResponse getOrderById(UUID id);

    Page<OrderWithUserInfoResponse> getOrdersWithFilters(LocalDateTime fromDate,
                                                         LocalDateTime toDate,
                                                         List<String> statuses,
                                                         Pageable pageable);

    List<OrderWithUserInfoResponse> getOrdersByUserId(UUID userId);

    OrderWithUserInfoResponse updateOrder(UUID id, UpdateOrderRequest request);

    void deleteOrder(UUID id);
}
