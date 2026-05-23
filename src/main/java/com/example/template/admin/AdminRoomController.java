package com.example.template.admin;

import com.example.template.venue.Room;
import com.example.template.venue.RoomDto;
import com.example.template.venue.RoomLayout;
import com.example.template.venue.RoomLayoutRepository;
import com.example.template.venue.RoomRepository;
import com.example.template.venue.RoomType;
import com.example.template.venue.RoomTypeRepository;
import com.example.template.venue.Venue;
import com.example.template.venue.VenueRepository;
import com.example.template.venue.VenueService;
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
    private final RoomTypeRepository roomTypeRepository;
    private final RoomLayoutRepository roomLayoutRepository;
    private final VenueService venueService;

    public AdminRoomController(
        RoomRepository roomRepository,
        VenueRepository venueRepository,
        RoomTypeRepository roomTypeRepository,
        RoomLayoutRepository roomLayoutRepository,
        VenueService venueService
    ) {
        this.roomRepository = roomRepository;
        this.venueRepository = venueRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomLayoutRepository = roomLayoutRepository;
        this.venueService = venueService;
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
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
            .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + request.roomTypeId()));
        RoomLayout roomLayout = roomLayoutRepository.findById(request.roomLayoutId())
            .orElseThrow(() -> new EntityNotFoundException("Room layout not found: " + request.roomLayoutId()));

        Room room = new Room();
        room.setVenue(venue);
        room.setRoomType(roomType);
        room.setRoomLayout(roomLayout);
        room.setName(request.name());
        applyLayout(room, roomLayout, request);
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
        if (request.roomTypeId() != null) {
            RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));
            room.setRoomType(roomType);
        }
        if (request.roomLayoutId() != null) {
            RoomLayout roomLayout = roomLayoutRepository.findById(request.roomLayoutId())
                .orElseThrow(() -> new EntityNotFoundException("Room layout not found"));
            room.setRoomLayout(roomLayout);
            applyLayout(room, roomLayout, request);
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
        return venueService.toRoomDto(room);
    }

    private void applyLayout(Room room, RoomLayout roomLayout, RoomRequest request) {
        room.setCapacity(request.capacity() != null ? request.capacity() : roomLayout.getCapacity());
        room.setRows(request.rows() != null ? request.rows() : roomLayout.getRows());
        room.setCols(request.cols() != null ? request.cols() : roomLayout.getCols());
    }

    public record RoomRequest(
        UUID venueId,
        UUID roomTypeId,
        UUID roomLayoutId,
        String name,
        Integer capacity,
        Integer rows,
        Integer cols
    ) {}
}
