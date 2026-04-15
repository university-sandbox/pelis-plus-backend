package com.example.template.seat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByScreeningId(UUID screeningId);

    List<Seat> findByIdInAndStatus(List<UUID> ids, String status);
}
