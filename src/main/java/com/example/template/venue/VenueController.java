package com.example.template.venue;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<List<VenueDto>> getVenues() {
        return ResponseEntity.ok(venueService.getVenues());
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomDto>> getRooms(@PathVariable UUID id) {
        return ResponseEntity.ok(venueService.getRooms(id));
    }
}
