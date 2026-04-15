package com.example.template.venue;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final RoomRepository roomRepository;

    public VenueService(VenueRepository venueRepository, RoomRepository roomRepository) {
        this.venueRepository = venueRepository;
        this.roomRepository = roomRepository;
    }

    public List<VenueDto> getVenues() {
        return venueRepository.findByActiveTrue().stream()
            .map(v -> new VenueDto(v.getId().toString(), v.getName(), v.getAddress(), v.getCity()))
            .toList();
    }

    public List<RoomDto> getRooms(UUID venueId) {
        return roomRepository.findByVenueIdAndActiveTrue(venueId).stream()
            .map(r -> new RoomDto(
                r.getId().toString(),
                r.getVenue().getId().toString(),
                r.getName(),
                r.getCapacity() != null ? r.getCapacity() : 0,
                r.getRows() != null ? r.getRows() : 0,
                r.getCols() != null ? r.getCols() : 0
            ))
            .toList();
    }

    public VenueDto toDto(Venue venue) {
        return new VenueDto(venue.getId().toString(), venue.getName(), venue.getAddress(), venue.getCity());
    }

    public RoomDto toRoomDto(Room room) {
        return new RoomDto(
            room.getId().toString(),
            room.getVenue().getId().toString(),
            room.getName(),
            room.getCapacity() != null ? room.getCapacity() : 0,
            room.getRows() != null ? room.getRows() : 0,
            room.getCols() != null ? room.getCols() : 0
        );
    }
}
