package com.example.template.movie;

import java.util.List;

public record MovieDto(
    Long id,
    String title,
    String overview,
    String posterPath,
    String backdropPath,
    String releaseDate,
    Double voteAverage,
    Integer voteCount,
    List<Long> genreIds,
    List<GenreDto> genres,
    Integer runtime,
    String status,
    String originalLanguage,
    Double popularity,
    Boolean adult,
    Boolean video,
    Boolean active
) {
}
