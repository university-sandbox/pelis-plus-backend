package com.example.template.order;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.screening.Screening;
import com.example.template.screening.ScreeningRepository;
import com.example.template.seat.Seat;
import com.example.template.seat.SeatRepository;
import com.example.template.snack.Snack;
import com.example.template.snack.SnackRepository;
import com.example.template.ticket.Ticket;
import com.example.template.ticket.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final OrderSnackRepository orderSnackRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final SnackRepository snackRepository;

    public OrderService(
        OrderRepository orderRepository,
        OrderTicketRepository orderTicketRepository,
        OrderSnackRepository orderSnackRepository,
        TicketRepository ticketRepository,
        AppUserRepository appUserRepository,
        ScreeningRepository screeningRepository,
        SeatRepository seatRepository,
        SnackRepository snackRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderTicketRepository = orderTicketRepository;
        this.orderSnackRepository = orderSnackRepository;
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
        this.snackRepository = snackRepository;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderPayload payload, UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);

        BigDecimal subtotal = BigDecimal.ZERO;

        Order savedOrder = orderRepository.save(order);

        // Create order tickets
        List<OrderTicket> orderTickets = new ArrayList<>();
        if (payload.tickets() != null) {
            for (CreateOrderPayload.CartTicketPayload tp : payload.tickets()) {
                UUID screeningId = UUID.fromString(tp.screeningId());
                UUID seatId = UUID.fromString(tp.seat().id());
                Screening screening = screeningRepository.findById(screeningId)
                    .orElseThrow(() -> new EntityNotFoundException("Screening not found: " + tp.screeningId()));
                Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + tp.seat().id()));

                OrderTicket ot = new OrderTicket();
                ot.setOrder(savedOrder);
                ot.setScreening(screening);
                ot.setSeat(seat);
                ot.setMovieTitle(screening.getMovie().getTitle());
                ot.setVenueName(screening.getRoom().getVenue().getName());
                ot.setRoomName(screening.getRoom().getName());
                ot.setScreeningDate(screening.getDate());
                ot.setScreeningTime(screening.getTime());
                ot.setFormat(screening.getFormat());
                ot.setPrice(screening.getPrice());
                orderTickets.add(orderTicketRepository.save(ot));

                subtotal = subtotal.add(screening.getPrice());

                // Mark seat as occupied
                seat.setStatus("occupied");
                seatRepository.save(seat);
            }
        }

        // Create order snacks
        if (payload.snacks() != null) {
            for (CreateOrderPayload.CartSnackItemPayload sp : payload.snacks()) {
                UUID snackId = UUID.fromString(sp.snack().id());
                Snack snack = snackRepository.findById(snackId)
                    .orElseThrow(() -> new EntityNotFoundException("Snack not found: " + sp.snack().id()));

                OrderSnack os = new OrderSnack();
                os.setOrder(savedOrder);
                os.setSnack(snack);
                os.setSnackName(snack.getName());
                os.setQuantity(sp.quantity() != null ? sp.quantity() : 1);
                os.setUnitPrice(snack.getPrice());
                os.setSelectedOptions(sp.selectedOptions() != null ? sp.selectedOptions().toString() : null);
                orderSnackRepository.save(os);

                subtotal = subtotal.add(snack.getPrice().multiply(BigDecimal.valueOf(os.getQuantity())));
            }
        }

        savedOrder.setSubtotal(subtotal);
        savedOrder.setDiscount(BigDecimal.ZERO);
        savedOrder.setTotal(subtotal);
        String formToken = "MOCK_IZIPAY_" + savedOrder.getId().toString().substring(0, 8).toUpperCase();
        savedOrder.setIzipayFormToken(formToken);
        orderRepository.save(savedOrder);

        return new CreateOrderResponse(savedOrder.getId().toString(), formToken);
    }

    @Transactional
    public OrderDto confirmOrder(UUID orderId, UUID userId, Object paymentResult) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        order.setStatus("confirmed");
        order.setPaymentStatus("approved");
        orderRepository.save(order);

        // Create tickets for each order ticket
        List<OrderTicket> orderTickets = orderTicketRepository.findByOrderId(orderId);
        for (OrderTicket ot : orderTickets) {
            String bookingCode = String.format("PP-%s", UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            String qrData = bookingCode + "|" + ot.getMovieTitle() + "|" + ot.getScreeningDate() + "|" + ot.getSeat().getRowLabel() + ot.getSeat().getColNum();

            Ticket ticket = new Ticket();
            ticket.setOrderTicket(ot);
            ticket.setOrder(order);
            ticket.setBookingCode(bookingCode);
            ticket.setQrData(qrData);
            ticketRepository.save(ticket);
        }

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getMyOrders(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
            .map(this::toDto)
            .toList();
    }

    private OrderDto toDto(Order order) {
        List<OrderTicket> tickets = orderTicketRepository.findByOrderId(order.getId());
        List<OrderSnack> snacks = orderSnackRepository.findByOrderId(order.getId());

        List<CartTicketDto> ticketDtos = tickets.stream().map(ot -> new CartTicketDto(
            ot.getId().toString(),
            ot.getScreening().getId().toString(),
            ot.getSeat().getId().toString(),
            ot.getMovieTitle(),
            ot.getVenueName(),
            ot.getRoomName(),
            ot.getScreeningDate().toString(),
            ot.getScreeningTime().toString(),
            ot.getFormat(),
            ot.getPrice() != null ? ot.getPrice().doubleValue() : 0.0,
            ot.getSeat().getRowLabel(),
            ot.getSeat().getColNum()
        )).toList();

        List<CartSnackItemDto> snackDtos = snacks.stream().map(os -> new CartSnackItemDto(
            os.getId().toString(),
            os.getSnack().getId().toString(),
            os.getSnackName(),
            os.getQuantity(),
            os.getUnitPrice() != null ? os.getUnitPrice().doubleValue() : 0.0,
            null
        )).toList();

        return new OrderDto(
            order.getId().toString(),
            order.getUser().getId().toString(),
            ticketDtos,
            snackDtos,
            order.getSubtotal() != null ? order.getSubtotal().doubleValue() : 0.0,
            order.getDiscount() != null ? order.getDiscount().doubleValue() : 0.0,
            order.getTotal() != null ? order.getTotal().doubleValue() : 0.0,
            order.getStatus(),
            order.getPaymentStatus(),
            order.getCreatedAt() != null ? order.getCreatedAt().toString() : null
        );
    }
}
