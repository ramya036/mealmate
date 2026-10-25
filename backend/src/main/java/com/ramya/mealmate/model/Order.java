package com.ramya.mealmate.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(unique = true)
    private String paymentIntentId;

    private Instant createdAt = Instant.now();

    @Version
    private long version;

    protected Order() {
    }

    public Order(User customer, String deliveryAddress) {
        this.customer = customer;
        this.deliveryAddress = deliveryAddress;
    }

    public void addItem(MenuItem menuItem, int quantity) {
        OrderItem item = new OrderItem(this, menuItem, quantity);
        items.add(item);
        total = total.add(item.lineTotal());
    }

    /** Amount in the smallest currency unit (cents), as Stripe expects. */
    public long totalInCents() {
        return total.movePointRight(2).longValueExact();
    }

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    public Instant getCreatedAt() { return createdAt; }
}
