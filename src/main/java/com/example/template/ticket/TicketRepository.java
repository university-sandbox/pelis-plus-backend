package com.example.template.ticket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByOrderUserId(UUID userId);

    Page<Ticket> findByOrderUserId(UUID userId, Pageable pageable);

    Optional<Ticket> findByIdAndOrderUserId(UUID id, UUID userId);

    List<Ticket> findByOrderId(UUID orderId);

    long countByOrderTicketScreeningId(UUID screeningId);
}
