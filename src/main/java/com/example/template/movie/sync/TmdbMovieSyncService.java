package com.example.template.movie.sync;

import com.example.template.config.TmdbProperties;
import com.example.template.movie.Genre;
import com.example.template.movie.GenreRepository;
import com.example.template.movie.Movie;
import com.example.template.movie.MovieRepository;
import com.example.template.screening.Screening;
import com.example.template.screening.ScreeningRepository;
import com.example.template.screening.ScreeningSeatGenerator;
import com.example.template.ticket.TicketRepository;
import com.example.template.venue.Room;
import com.example.template.venue.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TmdbMovieSyncService {

    private static final Logger logger = LoggerFactory.getLogger(TmdbMovieSyncService.class);
    private static final String NOW_PLAYING = "now_playing";
    private static final String UPCOMING = "upcoming";
    private static final String POPULAR = "popular";
    private static final String ACTIVE = "active";
    private static final String CANCELLED = "cancelled";
    private static final int GENERATED_SCREENINGS_PER_MOVIE = 3;
    private static final List<LocalTime> GENERATED_SCREENING_TIMES = List.of(
        LocalTime.of(15, 30),
        LocalTime.of(19, 0),
        LocalTime.of(21, 30)
    );

    private final TmdbClient tmdbClient;
    private final TmdbProperties properties;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ScreeningRepository screeningRepository;
    private final RoomRepository roomRepository;
    private final ScreeningSeatGenerator seatGenerator;
    private final TicketRepository ticketRepository;

    public TmdbMovieSyncService(
        TmdbClient tmdbClient,
        TmdbProperties properties,
        MovieRepository movieRepository,
        GenreRepository genreRepository,
        ScreeningRepository screeningRepository,
        RoomRepository roomRepository,
        ScreeningSeatGenerator seatGenerator,
        TicketRepository ticketRepository
    ) {
        this.tmdbClient = tmdbClient;
        this.properties = properties;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.screeningRepository = screeningRepository;
        this.roomRepository = roomRepository;
        this.seatGenerator = seatGenerator;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public MovieSyncStats syncMovies() {
        if (!properties.isEnabled()) {
            logger.info("TMDB movie sync is disabled");
            return new MovieSyncStats(0, 0, 0, 0, 0);
        }
        if (!properties.hasCredentials()) {
            logger.info("TMDB movie sync skipped because no TMDB credentials are configured");
            return new MovieSyncStats(0, 0, 0, 0, 0);
        }

        syncGenres();

        LocalDate today = LocalDate.now();
        Map<Long, TmdbMovieCandidate> candidates = fetchCandidates(today);
        List<Long> availableMovieIds = candidates.keySet().stream().toList();
        List<Long> nowPlayingMovieIds = candidates.values().stream()
            .filter(candidate -> NOW_PLAYING.equals(candidate.status()))
            .map(candidate -> candidate.summary().id())
            .toList();
        Set<Long> existingMovieIds = new HashSet<>(movieRepository.findAllById(availableMovieIds).stream()
            .map(Movie::getId)
            .toList());
        int upserted = 0;

        for (TmdbMovieCandidate candidate : candidates.values()) {
            Long movieId = candidate.summary().id();
            if (existingMovieIds.contains(movieId)) {
                continue;
            }

            try {
                TmdbMovieDetails details = tmdbClient.getMovieDetails(movieId);
                if (details == null || details.id() == null) {
                    continue;
                }
                upsertMovie(candidate.summary(), details, candidate.status());
                upserted++;
            } catch (RuntimeException ex) {
                logger.warn("Failed to sync TMDB movie {}", movieId, ex);
            }
        }

        int activated = activateCandidatesByStatus(candidates);
        int screeningsCancelled = cancelStaleCarteleraScreenings(nowPlayingMovieIds, today);
        int screeningsCreated = createMissingCarteleraScreenings(nowPlayingMovieIds, today);
        int deactivated = availableMovieIds.isEmpty()
            ? 0
            : movieRepository.deactivateMoviesNotInWithoutActiveScreenings(availableMovieIds);
        logger.info("TMDB movie sync availability updated: activated={}, screeningsCreated={}, screeningsCancelled={}",
            activated,
            screeningsCreated,
            screeningsCancelled
        );
        return new MovieSyncStats(candidates.size(), upserted, deactivated, screeningsCreated, screeningsCancelled);
    }

    private void syncGenres() {
        TmdbGenreResponse response = tmdbClient.getMovieGenres();
        if (response == null || response.genres() == null) {
            return;
        }

        List<Genre> genres = response.genres().stream()
            .filter(genre -> genre.id() != null && hasText(genre.name()))
            .map(tmdbGenre -> {
                Genre genre = genreRepository.findById(tmdbGenre.id()).orElseGet(Genre::new);
                genre.setId(tmdbGenre.id());
                genre.setName(tmdbGenre.name());
                return genre;
            })
            .toList();
        genreRepository.saveAll(genres);
    }

    private Map<Long, TmdbMovieCandidate> fetchCandidates(LocalDate today) {
        Map<Long, TmdbMovieCandidate> candidates = new LinkedHashMap<>();
        LocalDate earliestReleaseDate = today.minusDays(properties.resolvedReleaseLookbackDays());

        fetchMovieList(candidates, NOW_PLAYING, earliestReleaseDate, today, true);
        fetchMovieList(candidates, UPCOMING, today.plusDays(1), null, true);
        fetchMovieList(candidates, POPULAR, null, null, false);

        return candidates;
    }

    private void fetchMovieList(
        Map<Long, TmdbMovieCandidate> candidates,
        String listName,
        LocalDate earliestReleaseDate,
        LocalDate latestReleaseDate,
        boolean filterByReleaseDate
    ) {
        int maxPages = properties.resolvedMaxPagesPerList();
        int totalPages = 1;
        for (int page = 1; page <= Math.min(totalPages, maxPages); page++) {
            TmdbMoviePage response = tmdbClient.getMovieList(listName, page);
            if (response == null || response.results() == null) {
                break;
            }

            totalPages = response.totalPages() == null ? 1 : response.totalPages();
            for (TmdbMovieSummary summary : response.results()) {
                if (summary.id() == null) {
                    continue;
                }
                if (filterByReleaseDate && !isWithinReleaseWindow(summary.releaseDate(), earliestReleaseDate, latestReleaseDate)) {
                    continue;
                }
                candidates.putIfAbsent(summary.id(), new TmdbMovieCandidate(summary, listName));
            }
        }
    }

    private void upsertMovie(TmdbMovieSummary summary, TmdbMovieDetails details, String status) {
        Movie movie = movieRepository.findById(details.id()).orElseGet(Movie::new);
        movie.setId(details.id());
        movie.setTitle(firstText(details.title(), summary.title()));
        movie.setOverview(firstText(details.overview(), summary.overview()));
        movie.setPosterPath(firstText(details.posterPath(), summary.posterPath()));
        movie.setBackdropPath(firstText(details.backdropPath(), summary.backdropPath()));
        movie.setReleaseDate(firstText(details.releaseDate(), summary.releaseDate()));
        movie.setVoteAverage(firstValue(details.voteAverage(), summary.voteAverage()));
        movie.setVoteCount(firstValue(details.voteCount(), summary.voteCount()));
        movie.setRuntime(details.runtime());
        movie.setOriginalLanguage(firstText(details.originalLanguage(), summary.originalLanguage()));
        movie.setPopularity(firstValue(details.popularity(), summary.popularity()));
        movie.setAdult(firstValue(details.adult(), summary.adult()));
        movie.setVideo(firstValue(details.video(), summary.video()));
        movie.setTrailerYoutubeKey(findTrailerYoutubeKey(details.videos()));
        movie.setStatus(status);
        movie.setActive(true);
        movie.setGenres(resolveGenres(summary, details));
        movieRepository.save(movie);
    }

    private int activateCandidatesByStatus(Map<Long, TmdbMovieCandidate> candidates) {
        int activated = 0;
        activated += activateCandidatesByStatus(candidates, NOW_PLAYING);
        activated += activateCandidatesByStatus(candidates, UPCOMING);
        activated += activateCandidatesByStatus(candidates, POPULAR);
        return activated;
    }

    private int activateCandidatesByStatus(Map<Long, TmdbMovieCandidate> candidates, String status) {
        List<Long> ids = candidates.values().stream()
            .filter(candidate -> status.equals(candidate.status()))
            .map(candidate -> candidate.summary().id())
            .toList();
        return ids.isEmpty() ? 0 : movieRepository.activateMoviesIn(ids, status);
    }

    private int createMissingCarteleraScreenings(List<Long> movieIds, LocalDate today) {
        if (movieIds.isEmpty()) {
            return 0;
        }

        List<Room> rooms = roomRepository.findByActiveTrue().stream()
            .filter(room -> room.getRoomType() == null || Boolean.TRUE.equals(room.getRoomType().getActive()))
            .toList();
        if (rooms.isEmpty()) {
            logger.info("TMDB movie sync skipped screening generation because no active rooms are configured");
            return 0;
        }

        List<Movie> movies = movieRepository.findAllById(movieIds).stream()
            .filter(movie -> Boolean.TRUE.equals(movie.getActive()))
            .toList();
        int created = 0;
        int movieIndex = 0;
        for (Movie movie : movies) {
            if (screeningRepository.existsByMovieIdAndStatusAndDateGreaterThanEqual(movie.getId(), ACTIVE, today)) {
                continue;
            }

            for (int i = 0; i < GENERATED_SCREENINGS_PER_MOVIE; i++) {
                Room room = rooms.get((movieIndex + i) % rooms.size());
                Screening screening = new Screening();
                screening.setMovie(movie);
                screening.setRoom(room);
                screening.setDate(today.plusDays(i + 1L));
                screening.setTime(GENERATED_SCREENING_TIMES.get(i % GENERATED_SCREENING_TIMES.size()));
                screening.setFormat(resolveFormat(room));
                screening.setPrice(resolvePrice(room));
                screening.setStatus(ACTIVE);

                Screening saved = screeningRepository.save(screening);
                seatGenerator.generateSeats(saved, room);
                created++;
            }
            movieIndex++;
        }
        return created;
    }

    private int cancelStaleCarteleraScreenings(List<Long> currentNowPlayingMovieIds, LocalDate today) {
        if (currentNowPlayingMovieIds.isEmpty()) {
            return 0;
        }

        List<Screening> staleScreenings = screeningRepository.findActiveFutureScreeningsForMoviesNotIn(
            currentNowPlayingMovieIds,
            today
        );
        List<Screening> cancellableScreenings = staleScreenings.stream()
            .filter(screening -> ticketRepository.countByOrderTicketScreeningId(screening.getId()) == 0)
            .peek(screening -> screening.setStatus(CANCELLED))
            .toList();

        if (!cancellableScreenings.isEmpty()) {
            screeningRepository.saveAll(cancellableScreenings);
        }
        return cancellableScreenings.size();
    }

    private String resolveFormat(Room room) {
        if (room.getRoomType() != null && hasText(room.getRoomType().getCode())) {
            return room.getRoomType().getCode();
        }
        return "standard";
    }

    private BigDecimal resolvePrice(Room room) {
        String format = resolveFormat(room);
        if ("imax".equalsIgnoreCase(format)) {
            return BigDecimal.valueOf(38);
        }
        if ("3d".equalsIgnoreCase(format)) {
            return BigDecimal.valueOf(28);
        }
        return BigDecimal.valueOf(22);
    }

    private List<Genre> resolveGenres(TmdbMovieSummary summary, TmdbMovieDetails details) {
        List<Long> genreIds = new ArrayList<>();

        if (details.genres() != null && !details.genres().isEmpty()) {
            genreIds.addAll(details.genres().stream()
                .map(TmdbGenre::id)
                .filter(Objects::nonNull)
                .toList());
        } else if (summary.genreIds() != null) {
            genreIds.addAll(summary.genreIds());
        }

        return genreIds.isEmpty() ? List.of() : genreRepository.findAllById(genreIds);
    }

    private String findTrailerYoutubeKey(TmdbVideos videos) {
        if (videos == null || videos.results() == null) {
            return null;
        }

        return videos.results().stream()
            .filter(video -> hasText(video.key()))
            .filter(video -> "YouTube".equalsIgnoreCase(video.site()))
            .filter(video -> "Trailer".equalsIgnoreCase(video.type()))
            .sorted((left, right) -> Boolean.compare(Boolean.TRUE.equals(right.official()), Boolean.TRUE.equals(left.official())))
            .map(TmdbVideo::key)
            .findFirst()
            .orElse(null);
    }

    private String firstText(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    private <T> T firstValue(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private boolean isWithinReleaseWindow(String releaseDate, LocalDate earliestReleaseDate, LocalDate latestReleaseDate) {
        if (!hasText(releaseDate)) {
            return false;
        }

        try {
            LocalDate parsedReleaseDate = LocalDate.parse(releaseDate);
            boolean afterEarliest = earliestReleaseDate == null || !parsedReleaseDate.isBefore(earliestReleaseDate);
            boolean beforeLatest = latestReleaseDate == null || !parsedReleaseDate.isAfter(latestReleaseDate);
            return afterEarliest && beforeLatest;
        } catch (DateTimeParseException ex) {
            logger.warn("Ignoring TMDB movie with invalid release date {}", releaseDate);
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
