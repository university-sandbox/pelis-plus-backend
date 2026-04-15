package com.example.template.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTicketRepository extends JpaRepository<OrderTicket, UUID> {

    List<OrderTicket> findByOrderId(UUID orderId);
}
