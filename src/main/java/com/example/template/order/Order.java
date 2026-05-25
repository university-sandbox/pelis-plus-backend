package com.example.template.order;

import com.example.template.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "pending";

    @Column(nullable = false)
    private String paymentStatus = "pending";

    private String izipayFormToken;

    private String stripeCheckoutSessionId;

    @Column(length = 2000)
    private String stripeCheckoutUrl;

    @Column(nullable = false)
    private Integer membershipTicketsApplied = 0;

    @Column(nullable = false)
    private Boolean requiresPayment = true;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getIzipayFormToken() { return izipayFormToken; }
    public void setIzipayFormToken(String izipayFormToken) { this.izipayFormToken = izipayFormToken; }

    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) { this.stripeCheckoutSessionId = stripeCheckoutSessionId; }

    public String getStripeCheckoutUrl() { return stripeCheckoutUrl; }
    public void setStripeCheckoutUrl(String stripeCheckoutUrl) { this.stripeCheckoutUrl = stripeCheckoutUrl; }

    public Integer getMembershipTicketsApplied() { return membershipTicketsApplied; }
    public void setMembershipTicketsApplied(Integer membershipTicketsApplied) { this.membershipTicketsApplied = membershipTicketsApplied; }

    public Boolean getRequiresPayment() { return requiresPayment; }
    public void setRequiresPayment(Boolean requiresPayment) { this.requiresPayment = requiresPayment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
