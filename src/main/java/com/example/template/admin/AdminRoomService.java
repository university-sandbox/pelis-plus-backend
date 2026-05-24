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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRoomService {

    private final RoomRepository roomRepository;
    private final VenueRepository venueRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomLayoutRepository roomLayoutRepository;
    private final VenueService venueService;

    public AdminRoomService(
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

    @Transactional(readOnly = true)
    public Page<RoomDto> listRooms(int page) {
        return roomRepository.findAll(PageRequest.of(Math.max(0, page - 1), 20, Sort.by("name").ascending()))
            .map(venueService::toRoomDto);
    }

    @Transactional
    public RoomDto createRoom(AdminRoomController.RoomRequest request) {
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

        return venueService.toRoomDto(roomRepository.save(room));
    }

    @Transactional
    public RoomDto updateRoom(UUID id, AdminRoomController.RoomRequest request) {
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

        return venueService.toRoomDto(roomRepository.save(room));
    }

    @Transactional
    public RoomDto toggleActive(UUID id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));
        room.setActive(!Boolean.TRUE.equals(room.getActive()));
        return venueService.toRoomDto(roomRepository.save(room));
    }

    private void applyLayout(Room room, RoomLayout roomLayout, AdminRoomController.RoomRequest request) {
        room.setCapacity(request.capacity() != null ? request.capacity() : roomLayout.getCapacity());
        room.setRows(request.rows() != null ? request.rows() : roomLayout.getRows());
        room.setCols(request.cols() != null ? request.cols() : roomLayout.getCols());
    }
}
