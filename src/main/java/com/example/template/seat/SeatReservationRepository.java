package com.example.template.seat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, UUID> {

    List<SeatReservation> findBySeatIdAndExpiresAtAfter(UUID seatId, Instant now);

    List<SeatReservation> findBySeatIdInAndExpiresAtAfter(List<UUID> seatIds, Instant now);

    void deleteByExpiresAtBefore(Instant now);

    List<SeatReservation> findByUserIdAndExpiresAtAfter(UUID userId, Instant now);

    @Modifying
    @Query("DELETE FROM SeatReservation sr WHERE sr.user.id = :userId AND sr.seat.screening.id = :screeningId")
    void deleteByUserIdAndSeatScreeningId(@Param("userId") UUID userId, @Param("screeningId") UUID screeningId);
}
