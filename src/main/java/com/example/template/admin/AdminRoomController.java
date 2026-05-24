package com.example.template.admin;

import com.example.template.venue.RoomDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
@RequestMapping("/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    public AdminRoomController(AdminRoomService adminRoomService) {
        this.adminRoomService = adminRoomService;
    }

    @GetMapping
    public ResponseEntity<Page<RoomDto>> listRooms(
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(adminRoomService.listRooms(page));
    }

    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminRoomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable UUID id, @RequestBody RoomRequest request) {
        return ResponseEntity.ok(adminRoomService.updateRoom(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomDto> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(adminRoomService.toggleActive(id));
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
