package com.example.template.admin;

import com.example.template.venue.Room;
import com.example.template.venue.RoomDto;
import com.example.template.venue.RoomRepository;
import com.example.template.venue.Venue;
import com.example.template.venue.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final RoomRepository roomRepository;
    private final VenueRepository venueRepository;

    public AdminRoomController(RoomRepository roomRepository, VenueRepository venueRepository) {
        this.roomRepository = roomRepository;
        this.venueRepository = venueRepository;
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> listRooms() {
        List<RoomDto> rooms = roomRepository.findAll().stream()
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(rooms);
    }

    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@RequestBody RoomRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
            .orElseThrow(() -> new EntityNotFoundException("Venue not found: " + request.venueId()));

        Room room = new Room();
        room.setVenue(venue);
        room.setName(request.name());
        room.setCapacity(request.capacity());
        room.setRows(request.rows());
        room.setCols(request.cols());
        room.setActive(true);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(roomRepository.save(room)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable UUID id, @RequestBody RoomRequest request) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));

        if (request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new EntityNotFoundException("Venue not found"));
            room.setVenue(venue);
        }
        if (request.name() != null) room.setName(request.name());
        if (request.capacity() != null) room.setCapacity(request.capacity());
        if (request.rows() != null) room.setRows(request.rows());
        if (request.cols() != null) room.setCols(request.cols());

        return ResponseEntity.ok(toDto(roomRepository.save(room)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomDto> toggleActive(@PathVariable UUID id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));
        room.setActive(!Boolean.TRUE.equals(room.getActive()));
        return ResponseEntity.ok(toDto(roomRepository.save(room)));
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(
            room.getId().toString(),
            room.getVenue().getId().toString(),
            room.getName(),
            room.getCapacity() != null ? room.getCapacity() : 0,
            room.getRows() != null ? room.getRows() : 0,
            room.getCols() != null ? room.getCols() : 0
        );
    }

    public record RoomRequest(UUID venueId, String name, Integer capacity, Integer rows, Integer cols) {}
}
