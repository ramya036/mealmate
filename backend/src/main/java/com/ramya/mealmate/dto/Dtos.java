package com.ramya.mealmate.dto;

import com.ramya.mealmate.model.MenuItem;
import com.ramya.mealmate.model.Order;
import com.ramya.mealmate.model.OrderItem;
import com.ramya.mealmate.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response records for the REST API. Entities are never exposed directly. */
public final class Dtos {
    private Dtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UserResponse(Long id, String email, String fullName, String role) {
    }

    public record MenuItemRequest(
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.50") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @NotBlank String category,
            String imageUrl,
            boolean available) {
    }

    public record MenuItemResponse(Long id, String name, String description, BigDecimal price,
                                   String category, String imageUrl, boolean available) {
        public static MenuItemResponse from(MenuItem m) {
            return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(), m.getPrice(),
                    m.getCategory(), m.getImageUrl(), m.isAvailable());
        }
    }

    public record OrderLineRequest(@NotNull Long menuItemId, @Min(1) @Max(20) int quantity) {
    }

    public record CreateOrderRequest(
            @NotEmpty @Size(max = 30) List<@Valid OrderLineRequest> items,
            @NotBlank @Size(max = 300) String deliveryAddress) {
    }

    public record OrderLineResponse(Long menuItemId, String name, BigDecimal unitPrice, int quantity) {
        public static OrderLineResponse from(OrderItem i) {
            return new OrderLineResponse(i.getMenuItem().getId(), i.getItemName(), i.getUnitPrice(), i.getQuantity());
        }
    }

    public record OrderResponse(Long id, String customerEmail, OrderStatus status, BigDecimal total,
                                String deliveryAddress, Instant createdAt, List<OrderLineResponse> items) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getCustomer().getEmail(), o.getStatus(), o.getTotal(),
                    o.getDeliveryAddress(), o.getCreatedAt(),
                    o.getItems().stream().map(OrderLineResponse::from).toList());
        }
    }

    public record UpdateStatusRequest(@NotNull OrderStatus status) {
    }

    public record CheckoutResponse(Long orderId, String clientSecret, long amountCents, String currency) {
    }

    public record ErrorResponse(int status, String error, String message, Instant timestamp) {
    }
}
