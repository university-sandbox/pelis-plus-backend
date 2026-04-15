package com.example.template.ticket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByOrderUserId(UUID userId);

    Optional<Ticket> findByIdAndOrderUserId(UUID id, UUID userId);

    List<Ticket> findByOrderId(UUID orderId);
}
