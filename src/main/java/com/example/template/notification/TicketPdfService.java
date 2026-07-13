package com.example.template.notification;

import com.example.template.order.Order;
import com.example.template.ticket.Ticket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class TicketPdfService {

    private static final PDFontSet FONTS = new PDFontSet();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] createPdf(Order order, List<Ticket> tickets) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (Ticket ticket : tickets) {
                addTicketPage(document, order, ticket);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF de las entradas", exception);
        }
    }

    private void addTicketPage(PDDocument document, Order order, Ticket ticket) throws IOException {
        var orderTicket = ticket.getOrderTicket();
        var seat = orderTicket.getSeat();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float y = 780;
            y = write(content, "ENTRADA DIGITAL", 22, FONTS.bold, 40, y);
            y = write(content, "Pelis Plus", 14, FONTS.regular, 40, y - 8);
            y -= 18;
            y = write(content, truncate(orderTicket.getMovieTitle(), 70), 18, FONTS.bold, 40, y);
            y -= 20;
            y = write(content, "Codigo: " + ticket.getBookingCode(), 13, FONTS.bold, 40, y);
            y -= 22;
            y = write(content, "Cine: " + orderTicket.getVenueName(), 12, FONTS.regular, 40, y);
            y = write(content, "Sala: " + orderTicket.getRoomName(), 12, FONTS.regular, 40, y);
            y = write(content, "Fecha: " + orderTicket.getScreeningDate(), 12, FONTS.regular, 40, y);
            y = write(content, "Hora: " + orderTicket.getScreeningTime().format(TIME_FORMAT), 12, FONTS.regular, 40, y);
            y = write(content, "Formato: " + orderTicket.getFormat(), 12, FONTS.regular, 40, y);
            y = write(content, "Butaca: " + seat.getRowLabel() + seat.getColNum(), 12, FONTS.regular, 40, y);
            y -= 18;
            y = write(content, "Pedido: " + order.getId(), 10, FONTS.regular, 40, y);
            y -= 24;
            write(content, "Presenta este codigo en el cine para validar tu entrada.", 11, FONTS.regular, 40, y);
        }
    }

    private float write(
        PDPageContentStream content,
        String value,
        float size,
        PDType1Font font,
        float x,
        float y
    ) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(toPdfText(value));
        content.endText();
        return y - (size + 8);
    }

    private String truncate(String value, int maxLength) {
        String safeValue = toPdfText(value);
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength - 3) + "...";
    }

    private String toPdfText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static final class PDFontSet {
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }
}
