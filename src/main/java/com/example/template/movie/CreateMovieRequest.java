package com.example.template.movie;

import java.util.List;

public record CreateMovieRequest(
    Long id,
    String title,
    String overview,
    String posterPath,
    String backdropPath,
    String releaseDate,
    Double voteAverage,
    Integer voteCount,
    List<Long> genreIds,
    Integer runtime,
    String status,
    String originalLanguage,
    Double popularity,
    Boolean adult,
    Boolean video,
    String trailerYoutubeKey,
    Boolean active
) {
}
