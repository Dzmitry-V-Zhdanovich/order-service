package com.example.orderservice.service.impl;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderWithUserInfoResponse;
import com.example.orderservice.dto.response.UserInfoResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.client.UserServiceClient;
import com.example.orderservice.specification.OrderSpecification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderWithUserInfoResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for user: {}", request.getUserId());

        UserInfoResponse user = getUserWithCircuitBreaker(request.getUserId());

        Order order = orderMapper.toEntity(request);

        if (order.getItems() != null) {
            order.getItems().forEach(item -> item.setOrder(order));
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with id: {}", savedOrder.getId());

        return buildResponseWithUser(savedOrder, user);
    }

    @Override
    public OrderWithUserInfoResponse getOrderById(UUID id) {
        log.info("Fetching order by id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        UserInfoResponse user = getUserWithCircuitBreaker(order.getUserId());

        return buildResponseWithUser(order, user);
    }

    @Override
    public Page<OrderWithUserInfoResponse> getOrdersWithFilters(LocalDateTime fromDate,
                                                                LocalDateTime toDate,
                                                                List<String> statuses,
                                                                Pageable pageable) {
        log.info("Fetching orders with filters - fromDate: {}, toDate: {}, statuses: {}",
                fromDate, toDate, statuses);

        Specification<Order> spec = Specification
                .where(OrderSpecification.filterOrders(fromDate, toDate, statuses))
                .and((root, query, cb) -> cb.isFalse(root.get("deleted")));

        Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

        return ordersPage.map(order -> {
            UserInfoResponse user = getUserWithCircuitBreaker(order.getUserId());
            return buildResponseWithUser(order, user);
        });
    }

    @Override
    public List<OrderWithUserInfoResponse> getOrdersByUserId(UUID userId) {
        log.info("Fetching orders for user: {}", userId);

        // Проверяем существование пользователя
        UserInfoResponse user = getUserWithCircuitBreaker(userId);

        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .map(order -> buildResponseWithUser(order, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderWithUserInfoResponse updateOrder(UUID id, UpdateOrderRequest request) {
        log.info("Updating order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        UUID userId = order.getUserId();

        orderMapper.updateEntityFromRequest(order, request);

        if (request.getOrderItems() != null) {
            updateOrderItems(order, request.getOrderItems());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated successfully with id: {}", id);

        UserInfoResponse user = getUserWithCircuitBreaker(userId);
        return buildResponseWithUser(updatedOrder, user);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID id) {
        log.info("Soft deleting order with id: {}", id);

        if (orderRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }

        orderRepository.deleteById(id);
        log.info("Order soft deleted successfully with id: {}", id);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
    private UserInfoResponse getUserWithCircuitBreaker(UUID userId) {
        log.info("Calling UserService to get user info for id: {}", userId);
        return userServiceClient.getUserById(userId);
    }

    private void updateOrderItems(Order order, List<UpdateOrderRequest.OrderItemUpdateRequest> itemUpdates) {
        Map<UUID, OrderItem> existingItemsMap = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        for (UpdateOrderRequest.OrderItemUpdateRequest itemUpdate : itemUpdates) {
            if (itemUpdate.getId() != null && existingItemsMap.containsKey(itemUpdate.getId())) {
                OrderItem existingItem = existingItemsMap.get(itemUpdate.getId());
                existingItem.setId(itemUpdate.getItemId());
                existingItem.setQuantity(itemUpdate.getQuantity());
            } else if (itemUpdate.getId() == null) {
                OrderItem newItem = OrderItem.builder()
                        .id(itemUpdate.getItemId())
                        .quantity(itemUpdate.getQuantity())
                        .order(order)
                        .build();
                order.getItems().add(newItem);
            }
        }

        order.getItems().removeIf(item ->
                itemUpdates.stream().noneMatch(update ->
                        update.getId() != null && update.getId().equals(item.getId())
                )
        );
    }

    private OrderWithUserInfoResponse buildResponseWithUser(Order order, UserInfoResponse user) {
        OrderResponse orderResponse = orderMapper.toResponse(order);

        return OrderWithUserInfoResponse.builder()
                .order(orderResponse)
                .user(user)
                .build();
    }
}
