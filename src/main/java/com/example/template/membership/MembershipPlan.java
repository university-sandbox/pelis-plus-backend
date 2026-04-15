package com.example.template.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String validity;

    @Column(nullable = false)
    private Integer discountPercentage;

    @Column(nullable = false)
    private Integer ticketsPerMonth;

    private Boolean recommended = false;
    private String color;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getValidity() { return validity; }
    public void setValidity(String validity) { this.validity = validity; }

    public Integer getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Integer discountPercentage) { this.discountPercentage = discountPercentage; }

    public Integer getTicketsPerMonth() { return ticketsPerMonth; }
    public void setTicketsPerMonth(Integer ticketsPerMonth) { this.ticketsPerMonth = ticketsPerMonth; }

    public Boolean getRecommended() { return recommended; }
    public void setRecommended(Boolean recommended) { this.recommended = recommended; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
