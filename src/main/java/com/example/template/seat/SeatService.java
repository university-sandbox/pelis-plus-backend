package com.example.template.seat;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.screening.Screening;
import com.example.template.screening.ScreeningRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final ScreeningRepository screeningRepository;
    private final AppUserRepository appUserRepository;

    public SeatService(
        SeatRepository seatRepository,
        SeatReservationRepository seatReservationRepository,
        ScreeningRepository screeningRepository,
        AppUserRepository appUserRepository
    ) {
        this.seatRepository = seatRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.screeningRepository = screeningRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(UUID screeningId) {
        List<Seat> seats = seatRepository.findByScreeningId(screeningId);
        if (seats.isEmpty()) {
            throw new EntityNotFoundException("Screening not found: " + screeningId);
        }

        Instant now = Instant.now();
        // Get all reserved (not expired) seat IDs
        List<UUID> seatIds = seats.stream().map(Seat::getId).toList();
        List<SeatReservation> activeReservations = seatReservationRepository.findBySeatIdInAndExpiresAtAfter(seatIds, now);
        Set<UUID> reservedSeatIds = activeReservations.stream()
            .map(r -> r.getSeat().getId())
            .collect(Collectors.toSet());

        // Build seat map grouped by row
        List<String> rowLabels = seats.stream()
            .map(Seat::getRowLabel)
            .distinct()
            .sorted()
            .toList();

        List<Integer> colNums = seats.stream()
            .map(Seat::getColNum)
            .distinct()
            .sorted()
            .toList();

        // Group by row
        Map<String, List<Seat>> byRow = seats.stream()
            .collect(Collectors.groupingBy(Seat::getRowLabel));

        List<List<SeatDto>> seatMatrix = new ArrayList<>();
        for (String row : rowLabels) {
            List<Seat> rowSeats = byRow.getOrDefault(row, List.of());
            rowSeats = rowSeats.stream()
                .sorted(Comparator.comparingInt(Seat::getColNum))
                .toList();
            List<SeatDto> rowDtos = new ArrayList<>();
            for (Seat seat : rowSeats) {
                String effectiveStatus = seat.getStatus();
                if ("free".equals(effectiveStatus) && reservedSeatIds.contains(seat.getId())) {
                    effectiveStatus = "reserved";
                }
                rowDtos.add(new SeatDto(seat.getId().toString(), seat.getRowLabel(), seat.getColNum(), effectiveStatus, seat.getType()));
            }
            seatMatrix.add(rowDtos);
        }

        return new SeatMapDto(screeningId.toString(), rowLabels, colNums, seatMatrix);
    }

    @Transactional
    public ReservationResult reserveSeats(UUID screeningId, List<UUID> seatIds, UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Seat> seats = seatRepository.findByIdInAndStatus(seatIds, "free");
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more seats are not available");
        }

        // Check no active reservations exist for these seats
        Instant now = Instant.now();
        List<SeatReservation> existingReservations = seatReservationRepository.findBySeatIdInAndExpiresAtAfter(seatIds, now);
        if (!existingReservations.isEmpty()) {
            throw new IllegalArgumentException("One or more seats are already reserved");
        }

        Instant expiresAt = now.plus(15, ChronoUnit.MINUTES);

        for (Seat seat : seats) {
            SeatReservation reservation = new SeatReservation();
            reservation.setSeat(seat);
            reservation.setUser(user);
            reservation.setExpiresAt(expiresAt);
            reservation.setCreatedAt(now);
            seatReservationRepository.save(reservation);
        }

        return new ReservationResult(expiresAt);
    }

    @Transactional
    public void releaseSeats(UUID screeningId, UUID userId) {
        seatReservationRepository.deleteByUserIdAndSeatScreeningId(userId, screeningId);
    }

    public long countAvailableSeats(UUID screeningId) {
        List<Seat> seats = seatRepository.findByScreeningId(screeningId);
        Instant now = Instant.now();
        List<UUID> seatIds = seats.stream().map(Seat::getId).toList();
        if (seatIds.isEmpty()) return 0;
        List<SeatReservation> activeReservations = seatReservationRepository.findBySeatIdInAndExpiresAtAfter(seatIds, now);
        Set<UUID> reservedIds = activeReservations.stream()
            .map(r -> r.getSeat().getId())
            .collect(Collectors.toSet());
        return seats.stream()
            .filter(s -> "free".equals(s.getStatus()) && !reservedIds.contains(s.getId()))
            .count();
    }

    public record ReservationResult(Instant expiresAt) {}
}
