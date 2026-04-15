package com.example.template.admin;

import com.example.template.order.Order;
import com.example.template.order.OrderDto;
import com.example.template.order.OrderRepository;
import com.example.template.order.OrderService;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public AdminOrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> listOrders(
        @RequestParam(defaultValue = "1") int page
    ) {
        Page<OrderDto> orders = orderRepository.findAll(
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("createdAt").descending())
        ).map(o -> orderService.getOrder(o.getId(), o.getUser().getId()));
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return ResponseEntity.ok(orderService.getOrder(id, order.getUser().getId()));
    }
}
