package com.example.template.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.template.domain.AppUser;
import com.example.template.order.Order;
import com.example.template.order.OrderRepository;
import com.example.template.ticket.Ticket;
import com.example.template.ticket.TicketRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;

class OrderConfirmationEmailServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final TicketPdfService ticketPdfService = mock(TicketPdfService.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendsEveryConfirmationToTheConfiguredTestRecipient() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = order(orderId);
        Ticket ticket = new Ticket();
        ticket.setBookingCode("PP-ABC12345");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderId(orderId)).thenReturn(List.of(ticket));
        when(ticketPdfService.createPdf(order, List.of(ticket))).thenReturn(new byte[] {1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(message);

        service(true, "test@gmail.com").onOrderConfirmed(new OrderConfirmedEvent(orderId));

        verify(mailSender).send(message);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("test@gmail.com");
        assertThat(message.getFrom()).extracting(Object::toString).containsExactly("tickets@pelisplus.example");
    }

    @Test
    void doesNotSendWhenEmailIsDisabled() {
        service(false, "").onOrderConfirmed(new OrderConfirmedEvent(UUID.randomUUID()));

        verifyNoInteractions(orderRepository, ticketRepository, ticketPdfService, mailSender);
    }

    @Test
    void doesNotSendWhenNoTestRecipientIsConfigured() {
        service(true, "").onOrderConfirmed(new OrderConfirmedEvent(UUID.randomUUID()));

        verifyNoInteractions(orderRepository, ticketRepository, ticketPdfService, mailSender);
    }

    private OrderConfirmationEmailService service(boolean enabled, String testRecipient) {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername("verified@brevo.example");
        mailProperties.setPassword("smtp-key");
        return new OrderConfirmationEmailService(
            orderRepository,
            ticketRepository,
            ticketPdfService,
            mailSender,
            mailProperties,
            new EmailProperties(enabled, testRecipient, "tickets@pelisplus.example")
        );
    }

    private Order order(UUID orderId) {
        AppUser user = new AppUser();
        user.setName("Test User");
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        return order;
    }
}
