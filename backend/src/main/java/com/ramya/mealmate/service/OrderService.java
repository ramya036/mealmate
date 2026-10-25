package com.ramya.mealmate.service;

import com.ramya.mealmate.dto.Dtos.*;
import com.ramya.mealmate.exception.ApiException;
import com.ramya.mealmate.model.MenuItem;
import com.ramya.mealmate.model.Order;
import com.ramya.mealmate.model.OrderStatus;
import com.ramya.mealmate.model.User;
import com.ramya.mealmate.payment.PaymentGateway;
import com.ramya.mealmate.repository.MenuItemRepository;
import com.ramya.mealmate.repository.OrderRepository;
import com.ramya.mealmate.repository.UserRepository;
import com.ramya.mealmate.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /** Allowed status transitions for admins. Payment-driven transitions happen via the webhook only. */
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.PAID, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY),
            OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    private final OrderRepository orders;
    private final MenuItemRepository menu;
    private final UserRepository users;
    private final PaymentGateway payments;
    private final String currency;

    public OrderService(OrderRepository orders, MenuItemRepository menu, UserRepository users,
                        PaymentGateway payments, @Value("${mealmate.stripe.currency:usd}") String currency) {
        this.orders = orders;
        this.menu = menu;
        this.users = users;
        this.payments = payments;
        this.currency = currency;
    }

    @Transactional
    public OrderResponse create(AuthUser me, CreateOrderRequest req) {
        User customer = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        List<Long> ids = req.items().stream().map(OrderLineRequest::menuItemId).distinct().toList();
        Map<Long, MenuItem> found = menu.findAllById(ids).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        Order order = new Order(customer, req.deliveryAddress());
        for (OrderLineRequest line : req.items()) {
            MenuItem item = found.get(line.menuItemId());
            if (item == null || !item.isAvailable()) {
                throw ApiException.badRequest("Menu item " + line.menuItemId() + " is not available");
            }
            // Price always comes from the database, never from the client.
            order.addItem(item, line.quantity());
        }
        return OrderResponse.from(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> mine(AuthUser me) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(me.id()).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> all() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(AuthUser me, Long id) {
        return OrderResponse.from(loadOwned(me, id));
    }

    @Transactional
    public CheckoutResponse checkout(AuthUser me, Long id) {
        Order order = loadOwned(me, id);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("Order is already " + order.getStatus());
        }
        PaymentGateway.Intent intent = payments.createIntent(order.totalInCents(), currency, order.getId(),
                "order-" + order.getId());
        order.setPaymentIntentId(intent.id());
        return new CheckoutResponse(order.getId(), intent.clientSecret(), order.totalInCents(), currency);
    }

    @Transactional
    public OrderResponse cancel(AuthUser me, Long id) {
        Order order = loadOwned(me, id);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("Only unpaid orders can be cancelled by the customer");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus next) {
        Order order = orders.findWithItemsById(id).orElseThrow(() -> ApiException.notFound("Order"));
        if (!ADMIN_TRANSITIONS.get(order.getStatus()).contains(next)) {
            throw ApiException.conflict("Cannot move order from " + order.getStatus() + " to " + next);
        }
        order.setStatus(next);
        return OrderResponse.from(order);
    }

    /** Called from the verified Stripe webhook. Idempotent: Stripe may deliver the same event more than once. */
    @Transactional
    public void handlePaymentEvent(PaymentGateway.PaymentEvent event) {
        orders.findByPaymentIntentId(event.paymentIntentId()).ifPresentOrElse(order -> {
            if ("payment_intent.succeeded".equals(event.type()) && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.PAID);
                log.info("Order {} paid", order.getId());
            } else if ("payment_intent.payment_failed".equals(event.type())) {
                log.warn("Payment failed for order {}", order.getId());
            }
        }, () -> log.warn("No order for payment intent {}", event.paymentIntentId()));
    }

    private Order loadOwned(AuthUser me, Long id) {
        Order order = orders.findWithItemsById(id).orElseThrow(() -> ApiException.notFound("Order"));
        if (!me.isAdmin() && !order.getCustomer().getId().equals(me.id())) {
            // 404 rather than 403 so customers can't probe which order ids exist.
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }
}
