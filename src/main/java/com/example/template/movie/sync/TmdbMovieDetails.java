package com.example.template.movie.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record TmdbMovieDetails(
    Long id,
    String title,
    String overview,
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("backdrop_path") String backdropPath,
    @JsonProperty("release_date") String releaseDate,
    @JsonProperty("vote_average") Double voteAverage,
    @JsonProperty("vote_count") Integer voteCount,
    Integer runtime,
    List<TmdbGenre> genres,
    @JsonProperty("original_language") String originalLanguage,
    Double popularity,
    Boolean adult,
    Boolean video,
    TmdbVideos videos
) {
}
