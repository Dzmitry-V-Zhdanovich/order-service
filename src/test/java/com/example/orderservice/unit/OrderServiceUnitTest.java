package com.example.orderservice.unit;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderWithUserInfoResponse;
import com.example.orderservice.dto.response.UserInfoResponse;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.client.UserServiceClient;
import com.example.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
public class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    //@Spy
    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testItemId;
    private UUID testOrderItemId;

    private Order testOrder;
    private Item testItem;
    private OrderItem testOrderItem;
    private OrderResponse testOrderResponse;
    private UserInfoResponse testUserInfo;
    private OrderWithUserInfoResponse testOrderWithUserResponse;
    private CreateOrderRequest createOrderRequest;
    private UpdateOrderRequest updateOrderRequest;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testItemId = UUID.randomUUID();
        testOrderItemId = UUID.randomUUID();

        testItem = Item.builder()
                .id(testItemId)
                .name("Test Product")
                .price(BigDecimal.valueOf(75.25))
                .build();

        testOrder = Order.builder()
                .id(testOrderId)
                .userId(testUserId)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .deleted(false)
                .items(new ArrayList<>())
                .build();

        testOrderItem = OrderItem.builder()
                .id(testOrderItemId)
                .item(testItem)
                .quantity(2)
                .order(testOrder)
                .build();
        testOrder.getItems().add(testOrderItem);

        testOrderResponse = OrderResponse.builder()
                .id(testOrderId)
                .userId(testUserId)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        testUserInfo = UserInfoResponse.builder()
                .id(testUserId)
                .email("john.doe@example.com")
                .name("John")
                .surname("Doe")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testOrderWithUserResponse = OrderWithUserInfoResponse.builder()
                .order(testOrderResponse)
                .user(testUserInfo)
                .build();

        CreateOrderRequest.OrderItemRequest orderItemRequest = CreateOrderRequest.OrderItemRequest.builder()
                .itemId(testItemId)
                .quantity(2)
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .userId(testUserId)
                .status("PENDING")
                .totalPrice(BigDecimal.valueOf(150.50))
                .orderItems(List.of(orderItemRequest))
                .build();

        UpdateOrderRequest.OrderItemUpdateRequest orderItemUpdateRequest =
                UpdateOrderRequest.OrderItemUpdateRequest.builder()
                        .id(null)
                        .itemId(testItemId)
                        .quantity(3)
                        .build();

        updateOrderRequest = UpdateOrderRequest.builder()
                .status("CONFIRMED")
                .totalPrice(BigDecimal.valueOf(200.00))
                .orderItems(List.of(orderItemUpdateRequest))
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully when user exists")
        void createOrder_Success() {
            // Arrange
            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderMapper.toEntity(createOrderRequest)).thenReturn(testOrder);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            OrderWithUserInfoResponse result = orderService.createOrder(createOrderRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrder().getId()).isEqualTo(testOrderId);
            assertThat(result.getOrder().getUserId()).isEqualTo(testUserId);
            assertThat(result.getUser().getEmail()).isEqualTo("john.doe@example.com");

            verify(userServiceClient, times(1)).getUserById(testUserId);
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(orderMapper, times(1)).toEntity(createOrderRequest);
            verify(orderMapper, times(1)).toResponse(testOrder);
        }

        @Test
        @DisplayName("Should set order items relationship correctly with Item entity")
        void createOrder_SetsOrderItemsRelationship() {
            // Arrange
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderMapper.toEntity(createOrderRequest)).thenReturn(testOrder);
            when(orderRepository.save(orderCaptor.capture())).thenReturn(testOrder);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            orderService.createOrder(createOrderRequest);

            // Assert
            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getItems()).isNotEmpty();

            capturedOrder.getItems().forEach(item -> {
                assertThat(item.getOrder()).isEqualTo(capturedOrder);
                assertThat(item.getItem()).isNotNull();
                assertThat(item.getItem().getId()).isEqualTo(testItemId);
            });
        }
    }

    @Nested
    @DisplayName("Get Order By Id Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when exists")
        void getOrderById_Success() {
            // Arrange
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            OrderWithUserInfoResponse result = orderService.getOrderById(testOrderId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrder().getId()).isEqualTo(testOrderId);
            assertThat(result.getUser().getId()).isEqualTo(testUserId);

            assertThat(testOrder.getItems()).hasSize(1);
            assertThat(testOrder.getItems().getFirst().getItem()).isNotNull();
            assertThat(testOrder.getItems().getFirst().getItem().getId()).isEqualTo(testItemId);
            assertThat(testOrder.getItems().getFirst().getItem().getName()).isEqualTo("Test Product");
            assertThat(testOrder.getItems().getFirst().getItem().getPrice()).isEqualByComparingTo("75.25");

            verify(orderRepository, times(1)).findById(testOrderId);
            verify(userServiceClient, times(1)).getUserById(testUserId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order not found")
        void getOrderById_NotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(orderRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrderById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found with id: " + nonExistentId);

            verify(orderRepository, times(1)).findById(nonExistentId);
            verify(userServiceClient, never()).getUserById(any());
        }
    }

    @Nested
    @DisplayName("Get Orders With Filters Tests")
    class GetOrdersWithFiltersTests {

        @Test
        @DisplayName("Should return paginated orders with filters")
        void getOrdersWithFilters_Success() {
            // Arrange
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            LocalDateTime toDate = LocalDateTime.now();
            List<String> statuses = List.of("PENDING", "CONFIRMED");
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

            when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            Page<OrderWithUserInfoResponse> result = orderService.getOrdersWithFilters(
                    fromDate, toDate, statuses, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOrder().getId()).isEqualTo(testOrderId);

            Order retrievedOrder = testOrder;
            assertThat(retrievedOrder.getItems()).allMatch(item -> item.getItem() != null);
            assertThat(retrievedOrder.getItems()).allMatch(item -> item.getItem().getPrice() != null);

            verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(userServiceClient, times(1)).getUserById(testUserId);
        }
    }

    @Nested
    @DisplayName("Update Order Tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should update order successfully")
        void updateOrder_Success() {
            // Arrange
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            OrderWithUserInfoResponse result = orderService.updateOrder(testOrderId, updateOrderRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrder().getId()).isEqualTo(testOrderId);

            verify(orderRepository, times(1)).findById(testOrderId);
            verify(orderMapper, times(1)).updateEntityFromRequest(testOrder, updateOrderRequest);
            verify(orderRepository, times(1)).save(testOrder);
        }

        @Test
        @DisplayName("Should update existing order item with new quantity")
        void updateOrder_UpdatesExistingOrderItem() {
            // Arrange
            UUID existingItemId = testOrderItem.getId();

            UpdateOrderRequest.OrderItemUpdateRequest existingItemUpdate =
                    UpdateOrderRequest.OrderItemUpdateRequest.builder()
                            .id(existingItemId)
                            .itemId(testItemId)
                            .quantity(10)
                            .build();

            UpdateOrderRequest updateWithExistingItem = UpdateOrderRequest.builder()
                    .status("CONFIRMED")
                    .totalPrice(BigDecimal.valueOf(200.00))
                    .orderItems(List.of(existingItemUpdate))
                    .build();

            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderRepository.save(testOrder)).thenReturn(testOrder);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            orderService.updateOrder(testOrderId, updateWithExistingItem);

            // Assert
            verify(orderMapper, times(1)).updateEntityFromRequest(testOrder, updateWithExistingItem);
            assertThat(testOrder.getItems().get(0).getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should add new item to order when item update has null id")
        void updateOrder_AddsNewOrderItem() {
            // Arrange
            UUID newItemId = UUID.randomUUID();

            UpdateOrderRequest.OrderItemUpdateRequest newItemUpdate =
                    UpdateOrderRequest.OrderItemUpdateRequest.builder()
                            .id(null)
                            .itemId(newItemId)
                            .quantity(5)
                            .build();

            UpdateOrderRequest updateWithNewItem = UpdateOrderRequest.builder()
                    .status("CONFIRMED")
                    .totalPrice(BigDecimal.valueOf(250.00))
                    .orderItems(List.of(newItemUpdate))
                    .build();

            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(userServiceClient.getUserById(testUserId)).thenReturn(testUserInfo);
            when(orderRepository.save(testOrder)).thenReturn(testOrder);
            when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);

            // Act
            orderService.updateOrder(testOrderId, updateWithNewItem);

            // Assert
            verify(orderMapper, times(1)).updateEntityFromRequest(testOrder, updateWithNewItem);
        }
    }

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should soft delete order successfully")
        void deleteOrder_Success() {
            // Arrange
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            doNothing().when(orderRepository).deleteById(testOrderId);

            // Act
            orderService.deleteOrder(testOrderId);

            // Assert
            verify(orderRepository, times(1)).findById(testOrderId);
            verify(orderRepository, times(1)).deleteById(testOrderId);
        }
    }
}
