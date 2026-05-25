package com.example.template.order;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.membership.ActiveMembership;
import com.example.template.membership.ActiveMembershipRepository;
import com.example.template.payment.StripeCheckoutSession;
import com.example.template.payment.StripePaymentService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final ActiveMembershipRepository activeMembershipRepository;
    private final StripePaymentService stripePaymentService;

    public OrderService(
        OrderRepository orderRepository,
        OrderTicketRepository orderTicketRepository,
        OrderSnackRepository orderSnackRepository,
        TicketRepository ticketRepository,
        AppUserRepository appUserRepository,
        ScreeningRepository screeningRepository,
        SeatRepository seatRepository,
        SnackRepository snackRepository,
        ActiveMembershipRepository activeMembershipRepository,
        StripePaymentService stripePaymentService
    ) {
        this.orderRepository = orderRepository;
        this.orderTicketRepository = orderTicketRepository;
        this.orderSnackRepository = orderSnackRepository;
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
        this.snackRepository = snackRepository;
        this.activeMembershipRepository = activeMembershipRepository;
        this.stripePaymentService = stripePaymentService;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderPayload payload, UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal ticketSubtotal = BigDecimal.ZERO;

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
                ticketSubtotal = ticketSubtotal.add(screening.getPrice());

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
                os.setSelectedOptions(sp.selectedOptions());
                orderSnackRepository.save(os);

                subtotal = subtotal.add(snack.getPrice().multiply(BigDecimal.valueOf(os.getQuantity())));
            }
        }

        MembershipBenefitCalculation membershipBenefit = calculateMembershipBenefit(
            userId,
            orderTickets,
            ticketSubtotal
        );
        BigDecimal discount = membershipBenefit.totalDiscount();
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        boolean requiresPayment = total.compareTo(BigDecimal.ZERO) > 0;

        savedOrder.setSubtotal(subtotal);
        savedOrder.setDiscount(discount);
        savedOrder.setTotal(total);
        savedOrder.setMembershipTicketsApplied(membershipBenefit.freeTicketsApplied());
        savedOrder.setRequiresPayment(requiresPayment);
        savedOrder = orderRepository.save(savedOrder);

        StripeCheckoutSession checkoutSession = null;
        if (requiresPayment) {
            checkoutSession = stripePaymentService.createCheckoutSession(savedOrder);
            savedOrder.setStripeCheckoutSessionId(checkoutSession.id());
            savedOrder.setStripeCheckoutUrl(checkoutSession.url());
            savedOrder.setPaymentStatus("processing");
            savedOrder = orderRepository.save(savedOrder);
        }

        OrderDto orderDto = requiresPayment
            ? toDto(savedOrder)
            : confirmOrderInternal(savedOrder);

        return new CreateOrderResponse(
            savedOrder.getId().toString(),
            null,
            checkoutSession != null ? checkoutSession.id() : null,
            checkoutSession != null ? checkoutSession.url() : null,
            orderDto,
            requiresPayment,
            membershipBenefit.freeTicketsApplied(),
            total.doubleValue()
        );
    }

    @Transactional
    public OrderDto confirmOrder(UUID orderId, UUID userId, Object paymentResult) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if ("confirmed".equals(order.getStatus())) {
            return toDto(order);
        }

        if (Boolean.TRUE.equals(order.getRequiresPayment()) && order.getStripeCheckoutSessionId() != null) {
            throw new IllegalArgumentException("Use Stripe checkout confirmation for this order");
        }

        return confirmOrderInternal(order);
    }

    @Transactional
    public OrderDto confirmStripeCheckout(String sessionId, UUID userId) {
        Order order = orderRepository.findByStripeCheckoutSessionIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found for Stripe session: " + sessionId));

        if ("confirmed".equals(order.getStatus())) {
            return toDto(order);
        }

        StripeCheckoutSession checkoutSession = stripePaymentService.retrieveCheckoutSession(sessionId);
        if (!stripePaymentService.isPaidOrNoPaymentRequired(checkoutSession)) {
            throw new IllegalArgumentException("Stripe checkout session is not paid");
        }

        return confirmOrderInternal(order);
    }

    private OrderDto confirmOrderInternal(Order order) {
        consumeMembershipTickets(order);

        order.setStatus("confirmed");
        order.setPaymentStatus("approved");
        orderRepository.save(order);

        issueTickets(order);

        return toDto(order);
    }

    private void issueTickets(Order order) {
        if (!ticketRepository.findByOrderId(order.getId()).isEmpty()) {
            return;
        }

        // Create tickets for each order ticket
        List<OrderTicket> orderTickets = orderTicketRepository.findByOrderId(order.getId());
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
    }

    private MembershipBenefitCalculation calculateMembershipBenefit(
        UUID userId,
        List<OrderTicket> orderTickets,
        BigDecimal ticketSubtotal
    ) {
        Optional<ActiveMembership> activeMembership = getValidActiveMembership(userId);
        if (activeMembership.isEmpty() || orderTickets.isEmpty()) {
            return new MembershipBenefitCalculation(0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        ActiveMembership membership = activeMembership.get();
        int ticketsUsed = membership.getTicketsUsed() != null ? membership.getTicketsUsed() : 0;
        int ticketsTotal = membership.getPlan().getTicketsPerMonth() != null
            ? membership.getPlan().getTicketsPerMonth()
            : 0;
        int availableTickets = Math.max(0, ticketsTotal - ticketsUsed);
        int freeTicketsApplied = Math.min(availableTickets, orderTickets.size());

        BigDecimal freeTicketDiscount = orderTickets.stream()
            .map(OrderTicket::getPrice)
            .sorted(Comparator.reverseOrder())
            .limit(freeTicketsApplied)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidTicketSubtotal = ticketSubtotal.subtract(freeTicketDiscount).max(BigDecimal.ZERO);
        int discountPercentage = membership.getPlan().getDiscountPercentage() != null
            ? membership.getPlan().getDiscountPercentage()
            : 0;
        BigDecimal percentageDiscount = paidTicketSubtotal
            .multiply(BigDecimal.valueOf(discountPercentage))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new MembershipBenefitCalculation(
            freeTicketsApplied,
            freeTicketDiscount.setScale(2, RoundingMode.HALF_UP),
            percentageDiscount
        );
    }

    private void consumeMembershipTickets(Order order) {
        int ticketsToConsume = order.getMembershipTicketsApplied() != null
            ? order.getMembershipTicketsApplied()
            : 0;
        if (ticketsToConsume <= 0) {
            return;
        }

        ActiveMembership membership = getValidActiveMembership(order.getUser().getId())
            .orElseThrow(() -> new IllegalStateException("Active membership is no longer available"));

        int ticketsUsed = membership.getTicketsUsed() != null ? membership.getTicketsUsed() : 0;
        int ticketsTotal = membership.getPlan().getTicketsPerMonth() != null
            ? membership.getPlan().getTicketsPerMonth()
            : 0;
        if (ticketsTotal - ticketsUsed < ticketsToConsume) {
            throw new IllegalStateException("Not enough membership tickets available");
        }

        membership.setTicketsUsed(ticketsUsed + ticketsToConsume);
        BigDecimal currentDiscountUsed = membership.getDiscountUsed() != null
            ? membership.getDiscountUsed()
            : BigDecimal.ZERO;
        membership.setDiscountUsed(currentDiscountUsed.add(order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO));
        activeMembershipRepository.save(membership);
    }

    private Optional<ActiveMembership> getValidActiveMembership(UUID userId) {
        return activeMembershipRepository.findByUserId(userId)
            .filter(membership -> !membership.getExpiresAt().isBefore(LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getMyOrders(UUID userId, int page) {
        return orderRepository.findByUserId(
            userId,
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("createdAt").descending())
        ).map(this::toDto);
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
            os.getSelectedOptions()
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
            order.getMembershipTicketsApplied(),
            order.getRequiresPayment(),
            order.getCreatedAt() != null ? order.getCreatedAt().toString() : null
        );
    }

    private record MembershipBenefitCalculation(
        int freeTicketsApplied,
        BigDecimal freeTicketDiscount,
        BigDecimal percentageDiscount
    ) {
        BigDecimal totalDiscount() {
            return freeTicketDiscount.add(percentageDiscount).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
