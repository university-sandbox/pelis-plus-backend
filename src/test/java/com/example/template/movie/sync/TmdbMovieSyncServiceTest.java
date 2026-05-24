package com.example.template.movie.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.template.config.TmdbProperties;
import com.example.template.movie.GenreRepository;
import com.example.template.movie.Movie;
import com.example.template.movie.MovieRepository;
import com.example.template.screening.Screening;
import com.example.template.screening.ScreeningRepository;
import com.example.template.screening.ScreeningSeatGenerator;
import com.example.template.ticket.TicketRepository;
import com.example.template.venue.Room;
import com.example.template.venue.RoomRepository;
import com.example.template.venue.RoomType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TmdbMovieSyncServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ScreeningSeatGenerator seatGenerator;

    @Mock
    private TicketRepository ticketRepository;

    @Test
    void syncMoviesCreatesCarteleraScreeningsAndCancelsOnlyTicketlessStaleScreenings() {
        TmdbProperties properties = new TmdbProperties(
            true,
            null,
            "token",
            null,
            null,
            null,
            1,
            7,
            21,
            null,
            null
        );
        TmdbMovieSyncService service = new TmdbMovieSyncService(
            tmdbClient,
            properties,
            movieRepository,
            genreRepository,
            screeningRepository,
            roomRepository,
            seatGenerator,
            ticketRepository
        );

        LocalDate today = LocalDate.now();
        when(tmdbClient.getMovieGenres()).thenReturn(new TmdbGenreResponse(List.of()));
        when(tmdbClient.getMovieList("now_playing", 1)).thenReturn(new TmdbMoviePage(
            1,
            List.of(summary(10L, "Cartelera", today)),
            1
        ));
        when(tmdbClient.getMovieList("upcoming", 1)).thenReturn(new TmdbMoviePage(
            1,
            List.of(summary(20L, "Proximo", today.plusDays(10))),
            1
        ));
        when(tmdbClient.getMovieList("popular", 1)).thenReturn(new TmdbMoviePage(
            1,
            List.of(summary(30L, "Popular", today.minusDays(30))),
            1
        ));
        when(tmdbClient.getMovieDetails(10L)).thenReturn(details(10L, "Cartelera"));
        when(tmdbClient.getMovieDetails(20L)).thenReturn(details(20L, "Proximo"));
        when(tmdbClient.getMovieDetails(30L)).thenReturn(details(30L, "Popular"));
        when(movieRepository.findAllById(List.of(10L, 20L, 30L))).thenReturn(List.of());
        when(movieRepository.findAllById(List.of(10L))).thenReturn(List.of(movie(10L)));
        when(movieRepository.activateMoviesIn(List.of(10L), "now_playing")).thenReturn(1);
        when(movieRepository.activateMoviesIn(List.of(20L), "upcoming")).thenReturn(1);
        when(movieRepository.activateMoviesIn(List.of(30L), "popular")).thenReturn(1);
        when(movieRepository.deactivateMoviesNotInWithoutActiveScreenings(List.of(10L, 20L, 30L))).thenReturn(2);
        when(roomRepository.findByActiveTrue()).thenReturn(List.of(room("standard")));
        when(screeningRepository.existsByMovieIdAndStatusAndDateGreaterThanEqual(10L, "active", today)).thenReturn(false);
        when(screeningRepository.save(any(Screening.class))).thenAnswer(invocation -> {
            Screening screening = invocation.getArgument(0);
            screening.setId(UUID.randomUUID());
            return screening;
        });

        Screening ticketlessStale = screening(movie(99L));
        Screening ticketedStale = screening(movie(98L));
        when(screeningRepository.findActiveFutureScreeningsForMoviesNotIn(List.of(10L), today))
            .thenReturn(List.of(ticketlessStale, ticketedStale));
        when(ticketRepository.countByOrderTicketScreeningId(ticketlessStale.getId())).thenReturn(0L);
        when(ticketRepository.countByOrderTicketScreeningId(ticketedStale.getId())).thenReturn(1L);

        MovieSyncStats stats = service.syncMovies();

        assertThat(stats.fetched()).isEqualTo(3);
        assertThat(stats.upserted()).isEqualTo(3);
        assertThat(stats.deactivated()).isEqualTo(2);
        assertThat(stats.screeningsCreated()).isEqualTo(3);
        assertThat(stats.screeningsCancelled()).isEqualTo(1);
        assertThat(ticketlessStale.getStatus()).isEqualTo("cancelled");
        assertThat(ticketedStale.getStatus()).isEqualTo("active");

        ArgumentCaptor<Screening> createdScreenings = ArgumentCaptor.forClass(Screening.class);
        verify(screeningRepository, org.mockito.Mockito.times(3)).save(createdScreenings.capture());
        assertThat(createdScreenings.getAllValues())
            .allSatisfy(screening -> assertThat(screening.getMovie().getId()).isEqualTo(10L));
        verify(movieRepository).activateMoviesIn(List.of(10L), "now_playing");
        verify(movieRepository).activateMoviesIn(List.of(20L), "upcoming");
        verify(movieRepository).activateMoviesIn(List.of(30L), "popular");
        verify(screeningRepository).saveAll(List.of(ticketlessStale));
    }

    private TmdbMovieSummary summary(Long id, String title, LocalDate releaseDate) {
        return new TmdbMovieSummary(
            id,
            title,
            title + " overview",
            "/poster.jpg",
            "/backdrop.jpg",
            releaseDate.toString(),
            7.0,
            100,
            List.of(),
            "en",
            100.0,
            false,
            false
        );
    }

    private TmdbMovieDetails details(Long id, String title) {
        return new TmdbMovieDetails(
            id,
            title,
            title + " details",
            "/poster.jpg",
            "/backdrop.jpg",
            LocalDate.now().toString(),
            7.0,
            100,
            120,
            List.of(),
            "en",
            100.0,
            false,
            false,
            new TmdbVideos(List.of())
        );
    }

    private Movie movie(Long id) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle("Movie " + id);
        movie.setActive(true);
        return movie;
    }

    private Screening screening(Movie movie) {
        Screening screening = new Screening();
        screening.setId(UUID.randomUUID());
        screening.setMovie(movie);
        screening.setStatus("active");
        return screening;
    }

    private Room room(String code) {
        RoomType roomType = new RoomType();
        roomType.setCode(code);
        roomType.setActive(true);

        Room room = new Room();
        room.setRoomType(roomType);
        room.setRows(1);
        room.setCols(1);
        room.setActive(true);
        return room;
    }
}
