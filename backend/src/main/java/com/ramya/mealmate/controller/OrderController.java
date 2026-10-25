package com.ramya.mealmate.controller;

import com.ramya.mealmate.dto.Dtos.*;
import com.ramya.mealmate.security.AuthUser;
import com.ramya.mealmate.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateOrderRequest req) {
        return orders.create(me, req);
    }

    @GetMapping("/api/orders")
    public List<OrderResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return orders.mine(me);
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return orders.get(me, id);
    }

    @PostMapping("/api/orders/{id}/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return orders.checkout(me, id);
    }

    @PostMapping("/api/orders/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return orders.cancel(me, id);
    }

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> all() {
        return orders.all();
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return orders.updateStatus(id, req.status());
    }
}
