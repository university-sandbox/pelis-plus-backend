package com.example.template.movie;

import com.example.template.screening.ScreeningDto;
import com.example.template.screening.ScreeningService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;
    private final ScreeningService screeningService;

    public MovieController(MovieService movieService, ScreeningService screeningService) {
        this.movieService = movieService;
        this.screeningService = screeningService;
    }

    @GetMapping
    public ResponseEntity<MovieListResponse> getMovies(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long genre,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(movieService.getMovies(status, genre, search, page));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreDto>> getGenres() {
        return ResponseEntity.ok(movieService.getGenres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDto> getMovie(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovie(id));
    }

    @GetMapping("/{id}/screenings")
    public ResponseEntity<List<ScreeningDto>> getScreenings(
        @PathVariable Long id,
        @RequestParam(required = false) String venueId,
        @RequestParam(required = false) String date,
        @RequestParam(required = false) String format
    ) {
        return ResponseEntity.ok(screeningService.getForMovie(id, venueId, date, format));
    }
}
