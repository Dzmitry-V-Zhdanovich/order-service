package com.example.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {

    @Pattern(regexp = "^(PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Status must be PENDING, CONFIRMED, SHIPPED, DELIVERED, or CANCELLED")
    private String status;

    @DecimalMin(value = "0.01", message = "Total price must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Total price cannot exceed 999999.99")
    private BigDecimal totalPrice;

    @Valid
    private List<OrderItemUpdateRequest> orderItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemUpdateRequest {

        private UUID id;

        @NotNull(message = "Item ID cannot be null")
        @Positive(message = "Item ID must be positive")
        private UUID itemId;

        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 999, message = "Quantity cannot exceed 999")
        private Integer quantity;
    }
}
