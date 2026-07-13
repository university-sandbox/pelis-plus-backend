package com.example.template.notification;

import com.example.template.order.Order;
import com.example.template.order.OrderRepository;
import com.example.template.ticket.Ticket;
import com.example.template.ticket.TicketRepository;
import jakarta.mail.MessagingException;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

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
        helper.setText(buildPlainText(order, tickets), buildHtmlEmail(order, tickets));
        helper.addAttachment("entradas-" + order.getId() + ".pdf", new ByteArrayResource(ticketPdf));
        logger.info(
            "Sending confirmation email for order {} (recipient={}, smtpHost={}, smtpPort={}, startTls={}, ssl={})",
            order.getId(),
            "test-recipient",
            mailProperties.getHost(),
            mailProperties.getPort(),
            mailProperties.getProperties().get("mail.smtp.starttls.enable"),
            mailProperties.getProperties().get("mail.smtp.ssl.enable")
        );
        mailSender.send(message);
        logger.info("Confirmation email sent successfully for order {}", order.getId());
    }

    private String buildPlainText(Order order, List<Ticket> tickets) {
        return "Hola " + order.getUser().getName() + ",\n\n"
            + "Tu pago fue confirmado. Adjuntamos tus " + tickets.size() + " entrada(s) en PDF.\n"
            + "Codigo(s): " + tickets.stream().map(Ticket::getBookingCode).reduce((a, b) -> a + ", " + b).orElse("")
            + "\n\nEl PDF incluye el codigo QR para validar cada entrada en el cine.";
    }

    private String buildHtmlEmail(Order order, List<Ticket> tickets) {
        String ticketCards = tickets.stream().map(this::buildTicketCard).reduce("", String::concat);
        String countLabel = tickets.size() == 1 ? "entrada" : "entradas";

        return """
            <!doctype html>
            <html lang="es">
              <body style="margin:0; padding:0; background:#f3f5f7; color:#17212b; font-family:Arial, Helvetica, sans-serif;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f3f5f7; padding:32px 12px;">
                  <tr><td align="center">
                    <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="width:100%%; max-width:600px; background:#ffffff; border-radius:18px; overflow:hidden;">
                      <tr><td style="background:#111827; padding:30px 36px 26px;">
                        <p style="margin:0 0 10px; color:#f43f4d; font-size:12px; font-weight:700; letter-spacing:1.8px; text-transform:uppercase;">Pelis Plus</p>
                        <h1 style="margin:0; color:#ffffff; font-size:27px; line-height:34px;">Tu pago fue confirmado</h1>
                        <p style="margin:12px 0 0; color:#cbd5e1; font-size:15px; line-height:23px;">Tus entradas digitales están listas.</p>
                      </td></tr>
                      <tr><td style="padding:32px 36px 8px;">
                        <p style="margin:0; font-size:16px; line-height:25px;">Hola, <strong>%s</strong>.</p>
                        <p style="margin:12px 0 0; color:#52606d; font-size:15px; line-height:23px;">Adjuntamos tus %d %s en PDF. Cada una incluye un código QR para presentarlo al ingresar al cine.</p>
                      </td></tr>
                      <tr><td style="padding:24px 36px 8px;">%s</td></tr>
                      <tr><td style="padding:24px 36px 32px;">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#fff5f5; border-left:4px solid #ef4444; border-radius:4px;">
                          <tr><td style="padding:14px 16px; color:#7f1d1d; font-size:13px; line-height:20px;">Conserva el PDF adjunto y muestra el QR de cada entrada en el cine.</td></tr>
                        </table>
                      </td></tr>
                      <tr><td style="border-top:1px solid #e5e7eb; padding:20px 36px; color:#7b8794; font-size:12px; line-height:18px;">Orden %s · Este es un correo automático de Pelis Plus.</td></tr>
                    </table>
                  </td></tr>
                </table>
              </body>
            </html>
            """.formatted(
                escapeHtml(order.getUser().getName()),
                tickets.size(),
                countLabel,
                ticketCards,
                escapeHtml(order.getId().toString())
            );
    }

    private String buildTicketCard(Ticket ticket) {
        var orderTicket = ticket.getOrderTicket();
        String movie = orderTicket != null ? orderTicket.getMovieTitle() : "Entrada Pelis Plus";
        String venue = orderTicket != null ? orderTicket.getVenueName() : "";
        String date = orderTicket != null && orderTicket.getScreeningDate() != null ? orderTicket.getScreeningDate().toString() : "";
        String time = orderTicket != null && orderTicket.getScreeningTime() != null
            ? orderTicket.getScreeningTime().format(TIME_FORMAT)
            : "";
        String room = orderTicket != null ? orderTicket.getRoomName() : "";

        return """
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 16px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px;">
              <tr><td style="padding:18px 18px 12px;">
                <p style="margin:0; color:#111827; font-size:17px; font-weight:700; line-height:23px;">%s</p>
                <p style="margin:7px 0 0; color:#64748b; font-size:13px; line-height:19px;">%s · %s · %s</p>
              </td></tr>
              <tr><td style="padding:0 18px 18px;">
                <span style="display:inline-block; background:#111827; border-radius:6px; color:#ffffff; font-family:Consolas, monospace; font-size:13px; font-weight:700; letter-spacing:.4px; padding:8px 10px;">%s</span>
              </td></tr>
            </table>
            """.formatted(
                escapeHtml(movie),
                escapeHtml(date),
                escapeHtml(time),
                escapeHtml(venue + (room == null || room.isBlank() ? "" : " · " + room)),
                escapeHtml(ticket.getBookingCode())
            );
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
