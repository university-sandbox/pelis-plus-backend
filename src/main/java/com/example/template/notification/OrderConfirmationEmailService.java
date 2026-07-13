package com.example.template.notification;

import com.example.template.order.Order;
import com.example.template.order.OrderRepository;
import com.example.template.ticket.Ticket;
import com.example.template.ticket.TicketRepository;
import jakarta.mail.MessagingException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OrderConfirmationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(OrderConfirmationEmailService.class);

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketPdfService ticketPdfService;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailProperties emailProperties;

    public OrderConfirmationEmailService(
        OrderRepository orderRepository,
        TicketRepository ticketRepository,
        TicketPdfService ticketPdfService,
        JavaMailSender mailSender,
        MailProperties mailProperties,
        EmailProperties emailProperties
    ) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.ticketPdfService = ticketPdfService;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.emailProperties = emailProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        logger.info("Order confirmation email event received for order {}", event.orderId());
        if (!emailProperties.enabled()) {
            logger.info("Order confirmation email skipped for order {} because MAIL_ENABLED=false", event.orderId());
            return;
        }
        if (!emailProperties.hasTestRecipient()) {
            logger.error(
                "Order confirmation email skipped for order {}: MAIL_TEST_RECIPIENT is required when MAIL_ENABLED=true",
                event.orderId()
            );
            return;
        }
        if (!hasText(mailProperties.getUsername())) {
            logger.error("Order confirmation email skipped for order {}: MAIL_SMTP_USERNAME is required", event.orderId());
            return;
        }
        if (!hasText(mailProperties.getPassword())) {
            logger.error("Order confirmation email skipped for order {}: MAIL_SMTP_KEY is required", event.orderId());
            return;
        }
        if (!emailProperties.hasFromAddress()) {
            logger.error("Order confirmation email skipped for order {}: MAIL_FROM_ADDRESS is required", event.orderId());
            return;
        }

        try {
            logger.debug("Loading order and tickets to prepare confirmation email for order {}", event.orderId());
            Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));
            List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
            if (tickets.isEmpty()) {
                logger.warn("Order confirmation email skipped because order {} has no tickets", order.getId());
                return;
            }
            send(order, tickets, emailProperties.testRecipient());
        } catch (Exception exception) {
            // A successful Stripe checkout must remain confirmed if Brevo is unavailable.
            logger.error(
                "Order confirmation email failed for order {} ({}: {})",
                event.orderId(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
            );
        }
    }

    private void send(Order order, List<Ticket> tickets, String recipient) throws MessagingException {
        logger.debug("Generating PDF attachment for confirmation email of order {}", order.getId());
        byte[] ticketPdf = ticketPdfService.createPdf(order, tickets);
        logger.debug("PDF attachment generated for order {} ({} bytes)", order.getId(), ticketPdf.length);
        var message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(emailProperties.fromAddress());
        helper.setTo(recipient);
        helper.setSubject("Confirmacion de compra Pelis Plus - " + order.getId());
        helper.setText(
            "Hola " + order.getUser().getName() + ",\n\n"
                + "Tu pago fue confirmado. Adjuntamos tus " + tickets.size() + " entrada(s) en PDF.\n"
                + "Codigo(s): " + tickets.stream().map(Ticket::getBookingCode).reduce((a, b) -> a + ", " + b).orElse("")
                + "\n\nPresenta el codigo de tu entrada en el cine.",
            false
        );
        helper.addAttachment("entradas-" + order.getId() + ".pdf", new ByteArrayResource(ticketPdf));
        logger.info(
            "Sending confirmation email for order {} (recipient={}, smtpHost={}, smtpPort={})",
            order.getId(),
            "test-recipient",
            mailProperties.getHost(),
            mailProperties.getPort()
        );
        mailSender.send(message);
        logger.info("Confirmation email sent successfully for order {}", order.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
