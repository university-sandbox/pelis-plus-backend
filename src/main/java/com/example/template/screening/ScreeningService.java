package com.example.template.screening;

import com.example.template.movie.Movie;
import com.example.template.movie.MovieRepository;
import com.example.template.seat.SeatRepository;
import com.example.template.seat.SeatReservationRepository;
import com.example.template.seat.SeatService;
import com.example.template.venue.Room;
import com.example.template.venue.RoomDto;
import com.example.template.venue.RoomRepository;
import com.example.template.venue.Venue;
import com.example.template.venue.VenueDto;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final SeatService seatService;

    public ScreeningService(
        ScreeningRepository screeningRepository,
        MovieRepository movieRepository,
        RoomRepository roomRepository,
        SeatRepository seatRepository,
        SeatService seatService
    ) {
        this.screeningRepository = screeningRepository;
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.seatService = seatService;
    }

    public Page<ScreeningDto> getForMovie(Long movieId, String venueId, String date, String format, int page) {
        UUID venueUuid = (venueId != null && !venueId.isBlank()) ? UUID.fromString(venueId) : null;
        LocalDate screeningDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        String formatParam = (format != null && !format.isBlank()) ? format : null;

        return screeningRepository.findFiltered(
            movieId,
            venueUuid,
            screeningDate,
            formatParam,
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("date").ascending().and(Sort.by("time").ascending()))
        ).map(this::toDto);
    }

    public ScreeningDto getDetail(UUID id) {
        Screening screening = screeningRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Screening not found: " + id));
        return toDto(screening);
    }

    @Transactional
    public ScreeningDto createScreening(CreateScreeningRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
            .orElseThrow(() -> new EntityNotFoundException("Movie not found: " + request.movieId()));
        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new EntityNotFoundException("Room not found: " + request.roomId()));
        if (!Boolean.TRUE.equals(movie.getActive())) {
            throw new IllegalArgumentException("Cannot create screening for inactive movie");
        }
        if (!Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("Cannot create screening in inactive room");
        }
        if (room.getRoomType() != null && !Boolean.TRUE.equals(room.getRoomType().getActive())) {
            throw new IllegalArgumentException("Cannot create screening with inactive room type");
        }

        Screening screening = new Screening();
        screening.setMovie(movie);
        screening.setRoom(room);
        screening.setDate(request.date());
        screening.setTime(request.time());
        screening.setFormat(resolveFormat(request, room));
        screening.setPrice(request.price());
        screening.setStatus("active");

        Screening saved = screeningRepository.save(screening);

        // Generate seats for this screening
        generateSeats(saved, room);

        return toDto(saved);
    }

    @Transactional
    public ScreeningDto updateScreening(UUID id, CreateScreeningRequest request) {
        Screening screening = screeningRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Screening not found: " + id));

        if (request.movieId() != null) {
            Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new EntityNotFoundException("Movie not found"));
            if (!Boolean.TRUE.equals(movie.getActive())) {
                throw new IllegalArgumentException("Cannot assign inactive movie to screening");
            }
            screening.setMovie(movie);
        }
        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            if (!Boolean.TRUE.equals(room.getActive())) {
                throw new IllegalArgumentException("Cannot assign inactive room to screening");
            }
            screening.setRoom(room);
        }
        if (request.date() != null) screening.setDate(request.date());
        if (request.time() != null) screening.setTime(request.time());
        if (request.format() != null) screening.setFormat(request.format());
        if (request.format() == null && request.roomId() != null) {
            screening.setFormat(resolveFormat(request, screening.getRoom()));
        }
        if (request.price() != null) screening.setPrice(request.price());

        return toDto(screeningRepository.save(screening));
    }

    @Transactional
    public ScreeningDto cancelScreening(UUID id) {
        Screening screening = screeningRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Screening not found: " + id));
        screening.setStatus("cancelled");
        return toDto(screeningRepository.save(screening));
    }

    public Page<ScreeningDto> getAllScreenings(String status, Long movieId, int page) {
        return screeningRepository.findByFilters(
            (status != null && !status.isBlank()) ? status : null,
            movieId,
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("date").ascending().and(Sort.by("time").ascending()))
        ).map(this::toDto);
    }

    private void generateSeats(Screening screening, Room room) {
        int numRows = room.getRows() != null ? room.getRows() : 8;
        int numCols = room.getCols() != null ? room.getCols() : 10;

        for (int r = 0; r < numRows; r++) {
            String rowLabel = rowLabel(r);
            for (int c = 1; c <= numCols; c++) {
                com.example.template.seat.Seat seat = new com.example.template.seat.Seat();
                seat.setScreening(screening);
                seat.setRowLabel(rowLabel);
                seat.setColNum(c);
                seat.setStatus("free");
                // Preferential rows
                if (numRows == 10) {
                    seat.setType((rowLabel.equals("E") || rowLabel.equals("F")) ? "preferential" : "standard");
                } else {
                    seat.setType((rowLabel.equals("D") || rowLabel.equals("E")) ? "preferential" : "standard");
                }
                seatRepository.save(seat);
            }
        }
    }

    private String resolveFormat(CreateScreeningRequest request, Room room) {
        if (request.format() != null && !request.format().isBlank()) {
            return request.format();
        }
        if (room.getRoomType() != null) {
            return room.getRoomType().getCode();
        }
        return "standard";
    }

    private String rowLabel(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "R" + (index + 1);
    }

    ScreeningDto toDto(Screening screening) {
        Room room = screening.getRoom();
        Venue venue = room.getVenue();
        Movie movie = screening.getMovie();

        VenueDto venueDto = new VenueDto(venue.getId().toString(), venue.getName(), venue.getAddress(), venue.getCity());
        RoomDto roomDto = new RoomDto(
            room.getId().toString(),
            venue.getId().toString(),
            room.getName(),
            room.getCapacity() != null ? room.getCapacity() : 0,
            room.getRows() != null ? room.getRows() : 0,
            room.getCols() != null ? room.getCols() : 0,
            room.getActive(),
            room.getRoomType() != null
                ? new com.example.template.venue.RoomTypeDto(
                    room.getRoomType().getId().toString(),
                    room.getRoomType().getCode(),
                    room.getRoomType().getName(),
                    room.getRoomType().getDescription(),
                    room.getRoomType().getActive()
                )
                : null,
            room.getRoomLayout() != null
                ? new com.example.template.venue.RoomLayoutDto(
                    room.getRoomLayout().getId().toString(),
                    room.getRoomLayout().getName(),
                    room.getRoomLayout().getRows(),
                    room.getRoomLayout().getCols(),
                    room.getRoomLayout().getCapacity(),
                    room.getRoomLayout().getSeatMap(),
                    room.getRoomLayout().getActive()
                )
                : null
        );

        int totalSeats = seatRepository.findByScreeningId(screening.getId()).size();
        int availableSeats = (int) seatService.countAvailableSeats(screening.getId());

        return new ScreeningDto(
            screening.getId().toString(),
            movie.getId(),
            movie.getTitle(),
            venueDto,
            roomDto,
            screening.getDate().toString(),
            screening.getTime().toString(),
            screening.getFormat(),
            screening.getPrice() != null ? screening.getPrice().doubleValue() : 0.0,
            availableSeats,
            totalSeats,
            screening.getStatus()
        );
    }
}
