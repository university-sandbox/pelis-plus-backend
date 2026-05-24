package com.example.template.admin;

import com.example.template.venue.RoomLayout;
import com.example.template.venue.RoomLayoutDto;
import com.example.template.venue.RoomLayoutRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/room-layouts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomLayoutController {

    private final RoomLayoutRepository roomLayoutRepository;

    public AdminRoomLayoutController(RoomLayoutRepository roomLayoutRepository) {
        this.roomLayoutRepository = roomLayoutRepository;
    }

    @GetMapping
    public ResponseEntity<Page<RoomLayoutDto>> listRoomLayouts(
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(roomLayoutRepository.findAll(
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("name").ascending())
        ).map(this::toDto));
    }

    @PostMapping
    public ResponseEntity<RoomLayoutDto> createRoomLayout(@RequestBody RoomLayoutRequest request) {
        RoomLayout roomLayout = new RoomLayout();
        applyRequest(roomLayout, request);
        if (roomLayout.getActive() == null) roomLayout.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(roomLayoutRepository.save(roomLayout)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomLayoutDto> updateRoomLayout(
        @PathVariable UUID id,
        @RequestBody RoomLayoutRequest request
    ) {
        RoomLayout roomLayout = roomLayoutRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room layout not found: " + id));
        applyRequest(roomLayout, request);
        return ResponseEntity.ok(toDto(roomLayoutRepository.save(roomLayout)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomLayoutDto> toggleStatus(@PathVariable UUID id) {
        RoomLayout roomLayout = roomLayoutRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room layout not found: " + id));
        roomLayout.setActive(!Boolean.TRUE.equals(roomLayout.getActive()));
        return ResponseEntity.ok(toDto(roomLayoutRepository.save(roomLayout)));
    }

    private void applyRequest(RoomLayout roomLayout, RoomLayoutRequest request) {
        if (request.name() != null) roomLayout.setName(request.name());
        if (request.rows() != null) roomLayout.setRows(request.rows());
        if (request.cols() != null) roomLayout.setCols(request.cols());
        if (request.capacity() != null) roomLayout.setCapacity(request.capacity());
        if (request.seatMap() != null) roomLayout.setSeatMap(request.seatMap());
        if (request.active() != null) roomLayout.setActive(request.active());
    }

    private RoomLayoutDto toDto(RoomLayout roomLayout) {
        return new RoomLayoutDto(
            roomLayout.getId().toString(),
            roomLayout.getName(),
            roomLayout.getRows(),
            roomLayout.getCols(),
            roomLayout.getCapacity(),
            roomLayout.getSeatMap(),
            roomLayout.getActive()
        );
    }

    public record RoomLayoutRequest(
        String name,
        Integer rows,
        Integer cols,
        Integer capacity,
        String seatMap,
        Boolean active
    ) {}
}
