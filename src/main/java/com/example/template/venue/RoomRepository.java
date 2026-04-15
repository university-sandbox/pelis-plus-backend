package com.example.template.venue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByVenueIdAndActiveTrue(UUID venueId);

    List<Room> findByActiveTrue();
}
