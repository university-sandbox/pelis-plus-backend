package com.example.template.venue;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<Page<VenueDto>> getVenues(
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(venueService.getVenues(page));
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<Page<RoomDto>> getRooms(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(venueService.getRooms(id, page));
    }
}
