package com.example.template.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserId(UUID userId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Page<Order> findAll(Pageable pageable);
}
