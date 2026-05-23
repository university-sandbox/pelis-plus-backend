package com.example.template.venue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomLayoutRepository extends JpaRepository<RoomLayout, UUID> {

    List<RoomLayout> findByActiveTrue();
}
