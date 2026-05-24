package com.example.template.venue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByVenueIdAndActiveTrue(UUID venueId);

    Page<Room> findByVenueIdAndActiveTrue(UUID venueId, Pageable pageable);

    List<Room> findByActiveTrue();
}
