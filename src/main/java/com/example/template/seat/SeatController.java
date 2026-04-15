package com.example.template.seat;

import com.example.template.security.UserPrincipal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/screenings/{screeningId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public ResponseEntity<SeatMapDto> getSeatMap(@PathVariable UUID screeningId) {
        return ResponseEntity.ok(seatService.getSeatMap(screeningId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveSeats(
        @PathVariable UUID screeningId,
        @RequestBody ReserveSeatsRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        SeatService.ReservationResult result = seatService.reserveSeats(
            screeningId, request.seatIds(), principal.getUser().getId()
        );
        return ResponseEntity.ok(Map.of("expiresAt", result.expiresAt().toString()));
    }

    @DeleteMapping("/reserve")
    public ResponseEntity<Void> releaseSeats(
        @PathVariable UUID screeningId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        seatService.releaseSeats(screeningId, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
