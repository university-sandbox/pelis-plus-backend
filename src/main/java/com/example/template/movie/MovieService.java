package com.example.template.movie;

import com.example.template.screening.Screening;
import com.example.template.screening.ScreeningRepository;
import com.example.template.ticket.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private static final int PAGE_SIZE = 20;

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;

    public MovieService(
        MovieRepository movieRepository,
        GenreRepository genreRepository,
        ScreeningRepository screeningRepository,
        TicketRepository ticketRepository
    ) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
    }

    public MovieListResponse getMovies(String status, Long genreId, String search, int page) {
        int zeroBasedPage = Math.max(0, page - 1);
        Sort sort = Sort.by(
            Sort.Order.desc("releaseDate"),
            Sort.Order.desc("popularity"),
            Sort.Order.asc("title")
        );
        Pageable pageable = PageRequest.of(zeroBasedPage, PAGE_SIZE, sort);

        Page<Movie> moviePage;
        if (search != null && !search.isBlank()) {
            moviePage = movieRepository.findByActiveTrueAndTitleContainingIgnoreCase(search.trim(), pageable);
        } else if (genreId != null) {
            moviePage = movieRepository.findByActiveTrueAndGenreId(genreId, pageable);
        } else if (status != null && !status.isBlank()) {
            moviePage = movieRepository.findByActiveTrueAndStatus(status, pageable);
        } else {
            moviePage = movieRepository.findByActiveTrue(pageable);
        }

        List<MovieDto> results = moviePage.getContent().stream()
            .map(this::toDto)
            .toList();

        return new MovieListResponse(
            page,
            results,
            moviePage.getTotalPages(),
            (int) moviePage.getTotalElements()
        );
    }

    public List<MovieDto> getAdminMovies() {
        return movieRepository.findAllMovies(PageRequest.of(0, 200))
            .getContent().stream()
            .map(this::toDto)
            .toList();
    }

    public MovieDto getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Movie not found: " + id));
        return toDto(movie);
    }

    public List<GenreDto> getGenres() {
        return genreRepository.findAll(Sort.by("name")).stream()
            .map(g -> new GenreDto(g.getId(), g.getName()))
            .toList();
    }

    @Transactional
    public MovieDto createMovie(CreateMovieRequest request) {
        Movie movie = new Movie();
        applyRequest(movie, request);
        Movie saved = movieRepository.save(movie);
        return toDto(saved);
    }

    @Transactional
    public MovieDto updateMovie(Long id, CreateMovieRequest request) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Movie not found: " + id));
        applyRequest(movie, request);
        return toDto(movieRepository.save(movie));
    }

    @Transactional
    public MovieDto toggleStatus(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Movie not found: " + id));
        boolean newActive = !Boolean.TRUE.equals(movie.getActive());
        movie.setActive(newActive);
        if (!newActive) {
            cancelScreeningsWithoutSoldTickets(movie.getId());
        }
        return toDto(movieRepository.save(movie));
    }

    private void cancelScreeningsWithoutSoldTickets(Long movieId) {
        List<Screening> activeScreenings = screeningRepository.findByMovieIdAndStatus(movieId, "active");
        activeScreenings.stream()
            .filter(screening -> ticketRepository.countByOrderTicketScreeningId(screening.getId()) == 0)
            .forEach(screening -> screening.setStatus("cancelled"));
        screeningRepository.saveAll(activeScreenings);
    }

    private void applyRequest(Movie movie, CreateMovieRequest request) {
        if (request.id() != null) movie.setId(request.id());
        if (request.title() != null) movie.setTitle(request.title());
        if (request.overview() != null) movie.setOverview(request.overview());
        if (request.posterPath() != null) movie.setPosterPath(request.posterPath());
        if (request.backdropPath() != null) movie.setBackdropPath(request.backdropPath());
        if (request.releaseDate() != null) movie.setReleaseDate(request.releaseDate());
        if (request.voteAverage() != null) movie.setVoteAverage(request.voteAverage());
        if (request.voteCount() != null) movie.setVoteCount(request.voteCount());
        if (request.runtime() != null) movie.setRuntime(request.runtime());
        if (request.status() != null) movie.setStatus(request.status());
        if (request.originalLanguage() != null) movie.setOriginalLanguage(request.originalLanguage());
        if (request.popularity() != null) movie.setPopularity(request.popularity());
        if (request.adult() != null) movie.setAdult(request.adult());
        if (request.video() != null) movie.setVideo(request.video());
        if (request.trailerYoutubeKey() != null) movie.setTrailerYoutubeKey(request.trailerYoutubeKey());
        if (request.active() != null) movie.setActive(request.active());

        if (request.genreIds() != null) {
            List<Genre> genres = genreRepository.findAllById(request.genreIds());
            movie.setGenres(genres);
        }
    }

    public MovieDto toDto(Movie movie) {
        List<GenreDto> genreDtos = movie.getGenres().stream()
            .map(g -> new GenreDto(g.getId(), g.getName()))
            .toList();
        List<Long> genreIds = genreDtos.stream().map(GenreDto::id).toList();

        return new MovieDto(
            movie.getId(),
            movie.getTitle(),
            movie.getOverview(),
            movie.getPosterPath(),
            movie.getBackdropPath(),
            movie.getReleaseDate(),
            movie.getVoteAverage(),
            movie.getVoteCount(),
            genreIds,
            genreDtos,
            movie.getRuntime(),
            movie.getStatus(),
            movie.getOriginalLanguage(),
            movie.getPopularity(),
            movie.getAdult(),
            movie.getVideo(),
            movie.getTrailerYoutubeKey(),
            movie.getActive()
        );
    }
}
