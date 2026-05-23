package com.example.template.admin;

import com.example.template.venue.RoomType;
import com.example.template.venue.RoomTypeDto;
import com.example.template.venue.RoomTypeRepository;
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
@RequestMapping("/admin/room-types")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomTypeController {

    private final RoomTypeRepository roomTypeRepository;

    public AdminRoomTypeController(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeDto>> listRoomTypes() {
        return ResponseEntity.ok(roomTypeRepository.findAll().stream().map(this::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<RoomTypeDto> createRoomType(@RequestBody RoomTypeRequest request) {
        RoomType roomType = new RoomType();
        applyRequest(roomType, request);
        if (roomType.getActive() == null) roomType.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(roomTypeRepository.save(roomType)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomTypeDto> updateRoomType(@PathVariable UUID id, @RequestBody RoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + id));
        applyRequest(roomType, request);
        return ResponseEntity.ok(toDto(roomTypeRepository.save(roomType)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomTypeDto> toggleStatus(@PathVariable UUID id) {
        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + id));
        roomType.setActive(!Boolean.TRUE.equals(roomType.getActive()));
        return ResponseEntity.ok(toDto(roomTypeRepository.save(roomType)));
    }

    private void applyRequest(RoomType roomType, RoomTypeRequest request) {
        if (request.code() != null) roomType.setCode(request.code().trim().toLowerCase());
        if (request.name() != null) roomType.setName(request.name());
        if (request.description() != null) roomType.setDescription(request.description());
        if (request.active() != null) roomType.setActive(request.active());
    }

    private RoomTypeDto toDto(RoomType roomType) {
        return new RoomTypeDto(
            roomType.getId().toString(),
            roomType.getCode(),
            roomType.getName(),
            roomType.getDescription(),
            roomType.getActive()
        );
    }

    public record RoomTypeRequest(String code, String name, String description, Boolean active) {}
}
