package com.example.template.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSnackRepository extends JpaRepository<OrderSnack, UUID> {

    List<OrderSnack> findByOrderId(UUID orderId);
}
