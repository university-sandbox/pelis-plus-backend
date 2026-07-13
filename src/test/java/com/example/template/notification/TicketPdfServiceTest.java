package com.example.template.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.template.domain.AppUser;
import com.example.template.order.Order;
import com.example.template.order.OrderTicket;
import com.example.template.seat.Seat;
import com.example.template.ticket.Ticket;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

class TicketPdfServiceTest {

    private final TicketPdfService ticketPdfService = new TicketPdfService();

    @Test
    void createsOnePageWithAnEmbeddedQrImageForEachTicket() throws Exception {
        byte[] pdf = ticketPdfService.createPdf(order(), List.of(ticket()));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);

            int imageCount = 0;
            for (COSName name : document.getPage(0).getResources().getXObjectNames()) {
                if (document.getPage(0).getResources().getXObject(name) instanceof PDImageXObject) {
                    imageCount++;
                }
            }

            assertThat(imageCount).isGreaterThanOrEqualTo(1);
        }
    }

    private Order order() {
        AppUser user = new AppUser();
        user.setName("Test User");

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        return order;
    }

    private Ticket ticket() {
        Seat seat = new Seat();
        seat.setRowLabel("C");
        seat.setColNum(4);

        OrderTicket orderTicket = new OrderTicket();
        orderTicket.setMovieTitle("Película de prueba");
        orderTicket.setVenueName("Pelis Plus Centro");
        orderTicket.setRoomName("Sala 2");
        orderTicket.setScreeningDate(LocalDate.of(2026, 7, 13));
        orderTicket.setScreeningTime(LocalTime.of(19, 30));
        orderTicket.setFormat("2D");
        orderTicket.setPrice(BigDecimal.TEN);
        orderTicket.setSeat(seat);

        Ticket ticket = new Ticket();
        ticket.setOrderTicket(orderTicket);
        ticket.setBookingCode("PP-TEST123");
        ticket.setQrData("PP-TEST123|Película de prueba|2026-07-13|C4");
        return ticket;
    }
}
