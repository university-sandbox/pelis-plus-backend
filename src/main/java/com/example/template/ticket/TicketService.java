package com.example.template.ticket;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<TicketDto> getMyTickets(UUID userId) {
        return ticketRepository.findByOrderUserId(userId).stream()
            .map(this::toDto)
            .toList();
    }

    public TicketDto getTicket(UUID id, UUID userId) {
        Ticket ticket = ticketRepository.findByIdAndOrderUserId(id, userId)
            .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
        return toDto(ticket);
    }

    TicketDto toDto(Ticket ticket) {
        var ot = ticket.getOrderTicket();
        var seat = ot.getSeat();
        var order = ticket.getOrder();
        var user = order.getUser();
        var movie = ot.getScreening().getMovie();

        String seatLabel = seat.getRowLabel() + seat.getColNum();

        return new TicketDto(
            ticket.getId().toString(),
            order.getId().toString(),
            ticket.getBookingCode(),
            user.getName(),
            ot.getMovieTitle(),
            movie.getPosterPath(),
            ot.getVenueName(),
            ot.getRoomName(),
            ot.getScreeningDate().toString(),
            ot.getScreeningTime().toString(),
            seatLabel,
            ot.getFormat(),
            order.getTotal() != null ? order.getTotal().doubleValue() : 0.0,
            ticket.getQrData(),
            ticket.getIssuedAt().toString()
        );
    }
}
