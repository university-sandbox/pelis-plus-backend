// TODO: exponer endpoint para reenviar ticket por correo si el usuario lo pierde

package com.example.template.ticket;

import com.example.template.security.UserPrincipal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/me")
    public ResponseEntity<Page<TicketDto>> getMyTickets(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ticketService.getMyTickets(principal.getUser().getId(), page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getTicket(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ticketService.getTicket(id, principal.getUser().getId()));
    }
}
