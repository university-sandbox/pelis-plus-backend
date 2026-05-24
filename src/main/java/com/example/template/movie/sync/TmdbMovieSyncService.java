package com.example.template.movie.sync;

import com.example.template.config.TmdbProperties;
import com.example.template.movie.Genre;
import com.example.template.movie.GenreRepository;
import com.example.template.movie.Movie;
import com.example.template.movie.MovieRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TmdbMovieSyncService {

    private static final Logger logger = LoggerFactory.getLogger(TmdbMovieSyncService.class);
    private static final List<String> LISTS_TO_SYNC = List.of("now_playing", "upcoming");

    private final TmdbClient tmdbClient;
    private final TmdbProperties properties;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    public TmdbMovieSyncService(
        TmdbClient tmdbClient,
        TmdbProperties properties,
        MovieRepository movieRepository,
        GenreRepository genreRepository
    ) {
        this.tmdbClient = tmdbClient;
        this.properties = properties;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    @Transactional
    public MovieSyncStats syncMovies() {
        if (!properties.isEnabled()) {
            logger.info("TMDB movie sync is disabled");
            return new MovieSyncStats(0, 0, 0);
        }
        if (!properties.hasCredentials()) {
            logger.info("TMDB movie sync skipped because no TMDB credentials are configured");
            return new MovieSyncStats(0, 0, 0);
        }

        syncGenres();

        Map<Long, TmdbMovieCandidate> candidates = fetchCandidates();
        List<Long> activeIds = new ArrayList<>();
        int upserted = 0;

        for (TmdbMovieCandidate candidate : candidates.values()) {
            try {
                TmdbMovieDetails details = tmdbClient.getMovieDetails(candidate.summary().id());
                if (details == null || details.id() == null) {
                    continue;
                }
                upsertMovie(candidate.summary(), details, candidate.status());
                activeIds.add(details.id());
                upserted++;
            } catch (RuntimeException ex) {
                logger.warn("Failed to sync TMDB movie {}", candidate.summary().id(), ex);
            }
        }

        int deactivated = activeIds.isEmpty() ? 0 : movieRepository.deactivateMoviesNotIn(activeIds);
        return new MovieSyncStats(candidates.size(), upserted, deactivated);
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

    private Map<Long, TmdbMovieCandidate> fetchCandidates() {
        Map<Long, TmdbMovieCandidate> candidates = new LinkedHashMap<>();
        int maxPages = properties.resolvedMaxPagesPerList();

        for (String listName : LISTS_TO_SYNC) {
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
                    candidates.putIfAbsent(summary.id(), new TmdbMovieCandidate(summary, listName));
                }
            }
        }

        return candidates;
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
